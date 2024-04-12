package com.basketballranking.Crawler.services

import com.basketballranking.Crawler.models.Team import java.io.File


class GamesService(private val teamIdToNameMapper: Map<String, String>, iterationNumber: Int) {

    private val gamesCsv = File("games${iterationNumber}.csv")
    private val writtenContestIds = mutableSetOf<String>()

    fun writeGames(team: Team) {
        println("Writing games for team ${team.name} to CSV")
        for (season in team.seasons) {
            for (game in season.games) {
                if (!writtenContestIds.contains(game.contestId)) {
                    val node: List<String> = game.getTransitionNode()
                    val losingTeam = teamIdToNameMapper[node[0]]
                    val winningTeam = teamIdToNameMapper[node[1]]
                    gamesCsv.appendText("($losingTeam, $winningTeam)\n")
                }
            }
        }
    }
}