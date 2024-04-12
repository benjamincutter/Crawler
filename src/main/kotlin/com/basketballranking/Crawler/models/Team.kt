package com.basketballranking.Crawler.models

class Team(val name: String, val conference: String) {

    val seasons: MutableList<Season> = mutableListOf()

    override fun toString(): String {
        return "Team(name='$name', conference='$conference')"
    }

    fun addSeason(season: Season) {
        for (s in seasons) {
            if (s.seasonId == season.seasonId) {
                return
            }
        }
        seasons.add(season)
    }
}