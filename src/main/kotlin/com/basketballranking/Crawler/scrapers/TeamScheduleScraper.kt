package com.basketballranking.Crawler.scrapers

import com.basketballranking.Crawler.models.Game
import com.basketballranking.Crawler.models.Season
import com.basketballranking.Crawler.models.TeamHistoryInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.*

@Component
class TeamScheduleScraper() {
    private final val rootUrl = "https://stats.ncaa.org/"
    private val baseTeamUrl = rootUrl + "teams/"

    private final val log = LoggerFactory.getLogger(TeamScheduleScraper::class.java)

    fun scrapeSchedule(season: Season) {
        log.info("Scraping schedule for ${season.teamName} in season ${season.year}")
        val url = baseTeamUrl + season.seasonId
        val doc = Jsoup.connect(url).get()
        val tableElements = doc.getElementsByTag("tbody")[1].children()
        for (row in tableElements) {
            if (row.attributes().get("style").contains("border-bottom")) {
                continue;
            }
            try {
                season.addGame(parseGameRow(row, season.ncaaTeamId, season.teamName))
            } catch (e: Exception) {
                log.error("Error parsing game row: ${row.children().text()}")
            }
        }
        log.info("Recorded ${season.totalGames()} games for team ${season.teamName} in season ${season.year}")
    }

    fun peakTeamName(ncaaId: Int): String {
        val doc = Jsoup.connect(baseTeamUrl + ncaaId).get()
        return getTeamName(doc)
    }

    fun getSeasons(ncaaId: Int, earliestSeason: String): Map<String, Season> {
        val doc = Jsoup.connect(baseTeamUrl + ncaaId).get()
        val teamName = getTeamName(doc)
        val teamHistoryMap = getConference(doc, earliestSeason);
        val yearElements = doc.getElementById("year_list")?.children()
        val sportId = getSportId(doc)

        val yearMap =  mutableMapOf<String, Season>()
        if (yearElements != null) {
            for (year in yearElements) {
                val yearStr = year.text()
                if (isEarlierThanMinYear(yearStr, earliestSeason)) {
                    break
                }
                val yearId = year.attr("value")
                val teamHistoryInfo = teamHistoryMap[getStartYear(yearStr)]
                val curSeason = Season(
                    ncaaTeamId = ncaaId,
                    teamName = teamName,
                    year = yearStr,
                    seasonId = yearId,
                    sportId = sportId,
                    coach = teamHistoryInfo?.headCoach ?: "",
                    conference = teamHistoryInfo?.conference ?: "")
                yearMap[yearStr] = curSeason
            }
        }
        return yearMap
    }

    private fun parseGameRow(row: Element, teamId: Int, teamName: String): Game {
        val date = convertDateToTimestamp(row.children()[0].text())
        val isAway = row.children()[0].text().contains("@")
        val rawText = row.text()
        var competitor = row.children()[1].text()
        if (competitor.contains("@")) {
            competitor = competitor.replace("@ ", "")
        }
        var competitorId = row.children()[1].child(0).attr("href").split("/")[2]

        val boxScore = row.children()[2].text().split(" ")
        // if game was cancelled or postponed
        if (boxScore.size < 2) {
            return Game(
                homeTeamId = teamId.toString(),
                awayTeamId = competitorId,
                competitorName = competitor,
                homeScore = 0,
                awayScore = 0,
                date = date,
                contestId = "",
                gameText = rawText)
        }
        val score = boxScore[1].split("-")
        var contestId = ""
        try {
            contestId = row.children()[2].child(0).attr("href").split("/")[2]
        } catch(e: Exception)  {
            log.error("Error getting contestId for game on ${row.children().text()}")
        }
        if (isAway) {
            return Game(
                homeTeamId = competitorId,
                awayTeamId = teamId.toString(),
                awayTeamName = teamName,
                competitorName = competitor,
                homeScore = score[1].toInt(),
                awayScore = score[0].toInt(),
                date = date,
                contestId = contestId,
                gameText = rawText)
        } else {
            return Game(
                homeTeamId = teamId.toString(),
                homeTeamName = teamName,
                awayTeamId = competitorId,
                competitorName = competitor,
                homeScore = score[0].toInt(),
                awayScore = score[1].toInt(),
                date = date,
                contestId = contestId,
                gameText = rawText)

        }
    }

    private fun convertDateToTimestamp(dateStr: String): Long {
        val format = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val date = format.parse(dateStr)
        return date?.time ?: 0
    }

    private fun getTeamName(doc: Document): String {
        try {
            return doc.getElementsByAttributeValue("target", "ATHLETICS_URL")[0].text()
        } catch (e: Exception) {
            log.error("Error getting team name", doc.text())
            return ""
        }
    }

    private fun getConference(doc: Document, minYear: String): Map<String, TeamHistoryInfo> {
        val confUrl = doc.getElementsContainingText("Team History").attr("href")
        val confDoc = Jsoup.connect(rootUrl + confUrl).get()
        val conferenceRows = confDoc.getElementById("team_history_data_table")?.children()?.get(1)?.children()

        val conferenceMap = mutableMapOf<String, TeamHistoryInfo>()
        if (conferenceRows != null) {
            for (row in conferenceRows) {
                val year = row.children()[0].text()
                if(isEarlierThanMinYear(year, minYear)) {
                    break
                }
                val coach = row.children()[1].text()
                val conference = row.children()[3].text()
                conferenceMap[getStartYear(year)] = TeamHistoryInfo(year, coach, conference)
            }
        }
        return conferenceMap
    }

    private fun getSportId(doc: Document): String {
        val sportIdSelect = doc.getElementById("sport_list")
        val select = sportIdSelect.getElementsMatchingText("Men's Basketball")
        return select[1].attr("value")
    }

    private fun isEarlierThanMinYear(seasonYear: String, minYear: String): Boolean {
        val seasonYearInt = getStartYear(seasonYear).toInt()
        val minYearInt = getStartYear(minYear).toInt()
        return seasonYearInt < minYearInt
    }

    private fun getStartYear(year: String): String {
        return year.split("-")[0]
    }

}