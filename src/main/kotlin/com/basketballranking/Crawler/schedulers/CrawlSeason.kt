package com.basketballranking.Crawler.schedulers

import com.basketballranking.Crawler.constants.INCLUDED_CONFERENCES
import com.basketballranking.Crawler.constants.SEED_TEAMS
import com.basketballranking.Crawler.models.Game
import com.basketballranking.Crawler.models.Team
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import com.basketballranking.Crawler.scrapers.TeamScheduleScraper
import com.basketballranking.Crawler.services.DynamoTeamsService
import com.basketballranking.Crawler.services.GamesService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Component
class CrawlSeason : CommandLineRunner {

    @Autowired
    lateinit var teamScheduleScraper: TeamScheduleScraper

    @Autowired
    lateinit var dynamoDbTeamService: DynamoTeamsService

    val teamsToScrape = mutableSetOf<Int>()
    val completedTeams = mutableMapOf<String, Team>()

    // every season for a team has its own team id, so we need to map all ids to names
    val teamIdToNameMapper = mutableMapOf<String, String>()

    val capturedTeamIds = mutableSetOf<String>()

    // cut down on dupes
    val competitorNamesCaptured = mutableSetOf<String>()
    val missingConferences = mutableSetOf<String>()

    private val logger = LoggerFactory.getLogger(CrawlSeason::class.java)
    val earliestSeason = "2013-14"

    /**
     * Entrypoint for the crawler
     */
    override fun run(vararg args: String?) {
        logger.info("CrawlSeason is running")
        var csvIteration = 0
        seedTeamIds()

        // unfortunately, parellelism causes the site to block us
        val batchSize = 1
        while(teamsToScrape.isNotEmpty()) {
            logger.info("Teams to scrape: ${teamsToScrape.size}.  Teams Completed ${completedTeams.size}")
            val teamIds = teamsToScrape.take(batchSize)
            val jobList = mutableListOf<Job>()
            for (teamId in teamIds) {
                jobList.add(kotlinx.coroutines.GlobalScope.launch {
                    runTeam(teamId, earliestSeason)
                    teamsToScrape.remove(teamId)
                })
            }
            runBlocking {
                jobList.forEach { it.join() }
            }
            if (completedTeams.size % 50 == 0) {
                logger.info("Writing games to CSV")
                val gamesService = GamesService(teamIdToNameMapper, csvIteration)
                csvIteration++
                for (team in completedTeams.values) {
                    gamesService.writeGames(team)
                }
            }
        }

        logger.info("CrawlSeason is done")
    }

    /**
     * Run the team for the given team id
     */
    suspend fun runTeam(teamId: Int, earliestSeason: String) {
        val startTime = System.currentTimeMillis()
        var teamName = ""
        try {
            teamName = teamScheduleScraper.peakTeamName(teamId)
        } catch(e: Exception) {
            logger.error("Error getting team name for team id $teamId")
            return
        }
//         val dynamoSavedSeasons = dynamoDbTeamService.getTeamSeasons(teamName)

        teamIdToNameMapper[teamId.toString()] = teamName
        capturedTeamIds.add(teamId.toString())
        if (completedTeams.containsKey(teamName)) {
            logger.info("Team $teamName has already been scraped")
            return
        }
        val curTeam = Team(teamName, "")

        // get season ids, these still need to be enriched
        val allSeasons = teamScheduleScraper.getSeasons(teamId, earliestSeason)
        for (season in allSeasons.values) {
            logger.info("Conference: ${season.conference} for team $teamName")
            dynamoDbTeamService.addIdToTeamName(season.seasonId, teamName)
//            if (dynamoSavedSeasons.any { it.seasonYear == season.year }) {
//                logger.info("Skipping season ${season.seasonId} for team $teamName")
//                continue
//            }

            // don't scrape teams that aren't in the 32 conferences that get bids to tournament
            if (INCLUDED_CONFERENCES.contains(season.conference)) {
                teamScheduleScraper.scrapeSchedule(season)
                mapTeamNames(season.games)
                recordNewTeamsToScrape(season.games, teamId.toString())
                curTeam.addSeason(season)
                dynamoDbTeamService.processSeason(season)
            } else {
                missingConferences.add(season.conference)
                logger.info("Skipped conference ${season.conference}.  Current missing conferences: $missingConferences")
            }

            // remove any duplicate season ids that may have been added from other contests
            if (teamsToScrape.contains(season.seasonId.toInt())) {
                teamsToScrape.remove(season.seasonId.toInt())
            }
        }

        completedTeams[teamName] = curTeam
        logger.info("########### Scraped team $teamName in ${System.currentTimeMillis() - startTime} ms ###########")

    }

    private fun mapTeamNames(games: List<Game>) {
        for (game in games) {
            if (game.awayTeamName.isEmpty()) {
                game.awayTeamName = getTeamName(game.awayTeamId)
            }
            if (game.homeTeamName.isEmpty()) {
                game.homeTeamName = getTeamName(game.homeTeamId)
            }
        }
    }

    private fun getTeamName(teamId: String): String {
        // try local cache
        val teamName = teamIdToNameMapper[teamId]
        if (teamName != null) {
            return teamName
        }
        // try dynamo
        val dynamoTeam = dynamoDbTeamService.getTeamName(teamId)
        if (dynamoTeam.isNotEmpty()) {
            teamIdToNameMapper[teamId] = dynamoTeam
            return dynamoTeam
        }
        // map all relevant seasons.  this will be expensive at the beginning but will save time later
        logger.warn("Couldn't find $teamId in local cache or dynamo.  Mapping all seasons")
        val result = teamScheduleScraper.getSeasons(teamId.toInt(), earliestSeason)
        if (result.isNotEmpty()) {
            logger.info("Mapping team ${result[earliestSeason]?.teamName} with ${result.size} seasons")
            for (season in result.values) {
                dynamoDbTeamService.addIdToTeamName(season.seasonId, season.teamName)
                teamIdToNameMapper[teamId] = season.teamName
            }
            return teamIdToNameMapper[teamId] ?: ""
        }
        return ""
    }

    /**
     * Record the teams that need to be scraped for the next iteration
     */
    fun recordNewTeamsToScrape(games: List<Game>, teamId: String) {
        for (game in games) {
            if (competitorNamesCaptured.contains(game.competitorName)) {
                continue
            }
            if (teamId != game.homeTeamId && !capturedTeamIds.contains(game.homeTeamId)) {
                teamsToScrape.add(game.homeTeamId.toInt())
            }
            if (teamId != game.awayTeamId && !capturedTeamIds.contains(game.awayTeamId)) {
                teamsToScrape.add(game.awayTeamId.toInt())
            }
            competitorNamesCaptured.add(game.competitorName)
        }
    }

    /**
     * Seed the teams to scrape with the teams that are in the tournament
     */
    private fun seedTeamIds() {
        for (team in SEED_TEAMS) {
            teamsToScrape.add(team)
        }
    }
}