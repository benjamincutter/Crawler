package com.basketballranking.Crawler.services

import java.io.File

class CsvTeamRepository : TeamRepository {
    var csvFile =  File("teams.csv")

    // avoid duplicate games... but these actually are fine
    val writtenContestIds = mutableSetOf<Int>()

    init {
        if (!csvFile.exists()) {
            csvFile.createNewFile()
        }
    }

    override fun saveTeam(name: String, conference: String) {
        println("Saving team $name with conference $conference to CSV")
    }

    override fun getTeam(name: String): String {
        return "Getting team $name from CSV"
    }

    override fun getAllTeams(): List<String> {
        return listOf("Getting all teams from CSV")
    }

    override fun deleteTeam(name: String) {
        println("Deleting team $name from CSV")
    }

    override fun updateTeam(name: String, conference: String) {
        println("Updating team $name with conference $conference in CSV")
    }
}