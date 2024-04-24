package com.basketballranking.Crawler.models.dynamo

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

@DynamoDbBean
class GameStats {

    // default constructor for DynamoDB
    constructor()

    constructor(contestId: String, seasonYear: String, homeTeamId: String, homeTeamName: String, awayTeamId: String, awayTeamName: String, homeScore: Int, awayScore: Int, date: Long, byLine: String) {
        this.contestId = contestId
        this.seasonYear = seasonYear
        this.homeTeamId = homeTeamId
        this.homeTeamName = homeTeamName
        this.awayTeamId = awayTeamId
        this.awayTeamName = awayTeamName
        this.homeScore = homeScore
        this.awayScore = awayScore
        this.date = date
        this.byLine = byLine
    }
    var seasonYear: String = ""
        @DynamoDbPartitionKey get

    var contestId: String = ""
        @DynamoDbSortKey get

    var homeTeamId: String = ""
    var homeTeamName: String = ""

    var awayTeamId: String = ""
    var awayTeamName: String = ""

    var homeScore: Int = 0
    var awayScore: Int = 0

    var date: Long = 0

    var byLine: String = ""
}