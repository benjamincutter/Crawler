package com.basketballranking.Crawler.models

class Game(
        var homeTeamId: String,
        var homeTeamName: String = "",
        var awayTeamId: String,
        var awayTeamName: String = "",
        var competitorName: String,
        var homeScore: Int,
        var awayScore: Int,
        var date: Long,
        var neutralSite: Boolean = false,
        var contestId: String = "",
        var gameText: String = ""
    ) {





    fun isWin(teamId: String): Boolean {
        return if (teamId == homeTeamId) {
            homeScore > awayScore
        } else {
            awayScore > homeScore
        }
    }

    fun didTeamPlay(teamId: String): Boolean {
        return teamId.equals(homeTeamId) || teamId.equals(awayTeamId)
    }

    fun getTransitionNode(): List<String> {
        if (homeScore > awayScore) {
            return listOf(awayTeamId, homeTeamId)
        } else {
            return listOf(homeTeamId, awayTeamId)
        }
    }

    override fun toString(): String {
        return "Game(homeTeamId='$homeTeamId', awayTeamId='$awayTeamId', homeScore=$homeScore, awayScore=$awayScore, date=$date, neutralSite=$neutralSite, contestId='$contestId')"
    }
}