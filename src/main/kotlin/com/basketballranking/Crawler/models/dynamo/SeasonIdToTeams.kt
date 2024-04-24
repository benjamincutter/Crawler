package com.basketballranking.Crawler.models.dynamo

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

/**
 * Map to get season ids to team names, since teams have a season id
 * for each season they play in.
 * Names are used to generate game nodes.
 */
@DynamoDbBean
class SeasonIdToTeams {

    var ncaaSeasonId: String = ""
        @DynamoDbPartitionKey get

    var teamName = ""

    fun setAllFields(ncaaSeasonId: String, teamName: String) {
        this.ncaaSeasonId = ncaaSeasonId
        this.teamName = teamName
    }
}