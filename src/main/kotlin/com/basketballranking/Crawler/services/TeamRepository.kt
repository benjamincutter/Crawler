package com.basketballranking.Crawler.services

interface TeamRepository {
    fun saveTeam(name: String, conference: String)
    fun getTeam(name: String): String
    fun getAllTeams(): List<String>
    fun deleteTeam(name: String)
    fun updateTeam(name: String, conference: String)
}