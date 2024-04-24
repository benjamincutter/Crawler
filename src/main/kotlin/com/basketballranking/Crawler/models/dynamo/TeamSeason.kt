package com.basketballranking.Crawler.models.dynamo

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

@DynamoDbBean
class TeamSeason {

    // default constructor for dynamoDb
    constructor()

    constructor(name: String, seasonYear: String, conference: String, ncaaSeasonId: String, coach: String, contestIds: MutableList<String>) {
        this.name = name
        this.seasonYear = seasonYear
        this.conference = conference
        this.ncaaSeasonId = ncaaSeasonId
        this.coach = coach
        this.contestIds = contestIds
    }

    var name: String = ""
        @DynamoDbPartitionKey get

    var seasonYear: String = ""
        @DynamoDbSortKey get

    var conference: String = ""

    var ncaaSeasonId: String = ""

    var coach: String = ""

    var contestIds: MutableList<String> = mutableListOf()

    fun setAllFields(name: String, seasonYear: String, conference: String, ncaaSeasonId: String, coach: String, contestIds: MutableList<String>) {
        this.name = name
        this.seasonYear = seasonYear
        this.conference = conference
        this.ncaaSeasonId = ncaaSeasonId
        this.coach = coach
        this.contestIds = contestIds
    }
}