package com.basketballranking.Crawler.models

class Season (
    val ncaaTeamId: Int,
    val teamName: String,
    val year: String,
    val seasonId: String,
    val sportId: String,
    val conference: String,
    val coach: String) {
    val games: MutableList<Game> = mutableListOf()

    fun addGame(game: Game) {
        for (g in games) {
            if (g.contestId == game.contestId) {
                return
            }
        }
        games.add(game)
    }

    fun totalGames(): Int {
        return games.size
    }

    override fun toString(): String {
        return "Season(ncaaId=$ncaaTeamId, teamName=$teamName, year=$year, seasonId=$seasonId, conference=$conference, coach=$coach)"
    }
}