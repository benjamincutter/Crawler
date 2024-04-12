package com.basketballranking.Crawler.scrapers

import com.basketballranking.Crawler.models.Team
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AllTeamsScraper {
    val espn_url = "https://www.espn.com/mens-college-basketball/teams"

    val seedTeamIds = HashMap<String, Int>()

    init {
        createSeedTeamIds()
    }

    val log = LoggerFactory.getLogger(AllTeamsScraper::class.java)

    fun scrapeTeams(): MutableList<Team> {
        log.debug("Scraping teams from espn")
        val teams = mutableListOf<Team>()
        val doc = Jsoup.connect(espn_url).get()
        log.info(doc.title())
        val elements = doc.getElementsByClass("mt7")
        var seedsHit = 0
        for (element in elements) {
            val conference = element.getElementsByClass("headline").text()
            val teamElements = element.getElementsByClass("TeamLinks")
            for (teamElement in teamElements) {
                val teamName = teamElement.child(1).child(0).text()
                val team = Team(teamName, conference)
                if (seedTeamIds.containsKey(teamName)) {
                    seedsHit++

                }
                teams.add(team)
            }
        }
        log.info("Found ${teams.size} teams, $seedsHit seeded teams")
        return teams;
    }


    fun createSeedTeamIds() {
        // annoyingly, ncaa doesn't have conference info on the team page, so have to map
        // this function sets a seed team for each conference
        seedTeamIds.put("Virginia Cavaliers", 561220) // ACC
        seedTeamIds.put("Coppin State Eagles", 560853) // MEAC
        seedTeamIds.put("Buffalo Bulls", 560709) // MAC
        seedTeamIds.put("Kennesaw State Owls", 560616) // ASUN
        seedTeamIds.put("Illinois State Redbirds", 560616) // MVC
        seedTeamIds.put("Binghamton Bearcats", 560691) // America East
        seedTeamIds.put("Air Force Falcons", 560691) // Mountain West
        seedTeamIds.put("East Carolina Pirates", 560883) // American
        seedTeamIds.put("Fairleigh Dickinson Knights", 560897) // Northeast
        seedTeamIds.put("Davidson Wildcats", 560861) // Atalntic 10
        seedTeamIds.put("Lindenwood Lions", 560555) // Ohio Valley
        seedTeamIds.put("BYU Cougars", 560702) // Big 12
        seedTeamIds.put("Arizona State Sun Devils", 560669) // Pac-12
        seedTeamIds.put("Boston University Terriers", 560696) // Patriot
        seedTeamIds.put("UConn Huskies", 560851) // Big East
        seedTeamIds.put("Alabama Crimson Tide", 560658) // SEC
        seedTeamIds.put("Montana Grizzlies", 560785) // Big Sky
        seedTeamIds.put("Charleston Southern Buccaneers", 560685) // Big South
        seedTeamIds.put("Alabama A&M Bulldogs", 560654) // SWAC
        seedTeamIds.put("Illinois Fighting Illini", 560955) // Big Ten
        seedTeamIds.put("Chattanooga Mocs", 561164) // Southern
        seedTeamIds.put("Cal Poly Mustangs", 560712) // Big West
        seedTeamIds.put("Houston Christian Huskies", 560943) // Southland
        seedTeamIds.put("Campbell Fighting Camels", 560820) // Coastal
        seedTeamIds.put("North Dakota State Bison", 560820) // Summit
        seedTeamIds.put("Coastal Carolina Chanticleers", 560980) // Sun Belt
        seedTeamIds.put("Jacksonville State Gamecocks", 560571) // Conference USA
        seedTeamIds.put("Green Bay Phoenix", 561249) // Horizon
        seedTeamIds.put("Abilene Christian Wildcats", 560649) // WAC
        seedTeamIds.put("Chicago State Cougars", 560830) // independent
        seedTeamIds.put("Gonzaga Bulldogs", 560926) // West Coast
        seedTeamIds.put("Cornell Big Red", 560855) // Ivy
        seedTeamIds.put("Marist Red Foxes", 560740) // MAAC



    }
}