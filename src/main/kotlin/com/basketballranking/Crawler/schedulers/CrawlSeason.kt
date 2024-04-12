package com.basketballranking.Crawler.schedulers

import com.basketballranking.Crawler.models.Team
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import com.basketballranking.Crawler.scrapers.AllTeamsScraper
import com.basketballranking.Crawler.scrapers.TeamScheduleScraper
import com.basketballranking.Crawler.services.GamesService

@Component
class CrawlSeason : CommandLineRunner {


    @Autowired
    lateinit var allTeamsScraper: AllTeamsScraper

    @Autowired
    lateinit var teamScheduleScraper: TeamScheduleScraper

    val teamsToScrape = mutableSetOf<Int>()
    val completedTeams = mutableMapOf<String, Team>()

    // every season for a team has its own team id, so we need to map all ids to names
    val teamIdToNameMapper = mutableMapOf<String, String>()

    // cut down on dupes
    val competitorNamesCaptured = mutableSetOf<String>()

    val logger = LoggerFactory.getLogger(CrawlSeason::class.java)
    override fun run(vararg args: String?) {
        logger.info("CrawlSeason is running")
        var csvIteration = 0
        seedTeamIds()
        val earliestSeason = "2023-24"
        while(teamsToScrape.isNotEmpty()) {
            logger.info("Teams to scrape: ${teamsToScrape.size}.  Teams Completed ${completedTeams.size}")
            val teamId = teamsToScrape.first()
            teamsToScrape.remove(teamId)
            runTeam(teamId, earliestSeason)
            if (completedTeams.size % 20 == 0) {
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

    fun runTeam(teamId: Int, earliestSeason: String) {
        var teamName = ""
        try {
            teamName = teamScheduleScraper.peakTeamName(teamId)
        } catch(e: Exception) {
            logger.error("Error getting team name for team id $teamId")
            return
        }

        teamIdToNameMapper[teamId.toString()] = teamName
        if (completedTeams.containsKey(teamName)) {
            logger.info("Team $teamName has already been scraped")
            return
        }
        val curTeam = Team(teamName, "")
        val allSeasons = teamScheduleScraper.getSeasons(teamId, earliestSeason)
        for (season in allSeasons.values) {
            logger.info("Conference: ${season.conference} for team $teamName")
            teamScheduleScraper.scrapeSchedule(season)
            for (game in season.games) {
                if (competitorNamesCaptured.contains(game.competitorName)) {
                    logger.info("Already captured competitor name ${game.competitorName}")
                    continue
                }
                if (teamId.toString() != game.homeTeamId && !teamIdToNameMapper.containsKey(game.homeTeamId)) {
                    teamsToScrape.add(game.homeTeamId.toInt())
                }
                if (teamId.toString() != game.awayTeamId && !teamIdToNameMapper.containsKey(game.awayTeamId)) {
                    teamsToScrape.add(game.awayTeamId.toInt())
                }
                competitorNamesCaptured.add(game.competitorName)
            }
            curTeam.addSeason(season)
            if (teamsToScrape.contains(season.seasonId.toInt())) {
                logger.info("Removing dupe season id ${season.seasonId} for team $teamName")
                teamsToScrape.remove(season.seasonId.toInt())
            }
        }

        completedTeams[teamName] = curTeam
    }

    fun seedTeamIds() {
        teamsToScrape.add(561220) // ACC    Virginia Cavaliers
        teamsToScrape.add(560853) // MEAC   Coppin State Eagles
        teamsToScrape.add(560709) // MAC Buffalo Bulls
        teamsToScrape.add(560616) // ASUN   Kennesaw State Owls
        teamsToScrape.add(560616) // MVC    Illinois State Redbirds
        teamsToScrape.add(560691) // America East   Binghamton Bearcats
        teamsToScrape.add(560691) // Mountain West  Air Force Falcons
        teamsToScrape.add(560883) // American   East Carolina Pirates
        teamsToScrape.add(560897) // Northeast  Fairleigh Dickinson Knights
        teamsToScrape.add(560861) // Atalntic 10    Davidson Wildcats
        teamsToScrape.add(560555) // Ohio Valley    Lindenwood Lions
        teamsToScrape.add(560702) // Big 12 BYU Cougars
        teamsToScrape.add(560669) // Pac-12 Arizona State Sun Devils
        teamsToScrape.add(560696) // Patriot    Boston University Terriers
        teamsToScrape.add(560851) // Big East   UConn Huskies
        teamsToScrape.add(560658) // SEC    Alabama Crimson Tide
        teamsToScrape.add(560785) // Big Sky    Montana Grizzlies
        teamsToScrape.add(560685) // Big South  Charleston Southern Buccaneers
        teamsToScrape.add(560654) // SWAC   Alabama A&M Bulldogs
        teamsToScrape.add(560955) // Big Ten    Illinois Fighting Illini
        teamsToScrape.add(561164) // Southern   Chattanooga Mocs
        teamsToScrape.add(560712) // Big West   Cal Poly Mustangs
        teamsToScrape.add(560943) // Southland  Houston Christian Huskies
        teamsToScrape.add(560820) // Coastal    Campbell Fighting Camels
        teamsToScrape.add(560820) // Summit League  Denver Pioneers
        teamsToScrape.add(560980) // Sun Belt   Georgia State Panthers
        teamsToScrape.add(560571) // Conference USA Florida Atlantic Owls
        teamsToScrape.add(561249) // Horizon    Green Bay Phoenix
        teamsToScrape.add(560649) // WAC    Abilene Christian Wildcats
        teamsToScrape.add(560830) // independent    Chicago State Cougars
        teamsToScrape.add(560926) // West Coast Gonzaga Bulldogs
        teamsToScrape.add(560855) // Ivy    Cornell Big Red
        teamsToScrape.add(560740) // MAAC   Marist Red Foxes
    }

}