package com.basketballranking.Crawler.services


import com.basketballranking.Crawler.models.Season
import com.basketballranking.Crawler.models.dynamo.GameStats
import com.basketballranking.Crawler.models.dynamo.SeasonIdToTeams
import com.basketballranking.Crawler.models.dynamo.TeamSeason
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

@Service
class DynamoTeamsService {

    private final val logger = LoggerFactory.getLogger(DynamoTeamsService::class.java)

    private final val enhancedClient: DynamoDbEnhancedClient
    private final val teamSeasonsTable: DynamoDbTable<TeamSeason>
    private final val seasonIdToTeamsTable: DynamoDbTable<SeasonIdToTeams>
    private final val gameStatsTable: DynamoDbTable<GameStats>

    init {
        val client = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .build()
        enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
        teamSeasonsTable = enhancedClient.table("TeamSeason", TableSchema.fromClass(TeamSeason::class.java))
        seasonIdToTeamsTable = enhancedClient.table("SeasonIdToTeams", TableSchema.fromClass(SeasonIdToTeams::class.java))
        gameStatsTable = enhancedClient.table("GameStats", TableSchema.fromClass(GameStats::class.java))

    }

    suspend fun getTeamSeasons(teamName: String): List<TeamSeason> {
        logger.info("Getting team $teamName")
        val queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(teamName).build())
        val pageResults: PageIterable<TeamSeason>  = teamSeasonsTable.query(queryConditional)
        val seasons = mutableListOf<TeamSeason>()
        pageResults.items().stream().forEach { season -> seasons.add(season) }
        return seasons;
    }

    fun addTeamSeason(teamSeason: TeamSeason) {
        teamSeasonsTable.putItem(teamSeason)

        val teamName = teamSeason.name
        val ncaaId = teamSeason.ncaaSeasonId
        val seasonIdToTeam = SeasonIdToTeams()
        seasonIdToTeam.setAllFields(ncaaId, teamName)
        seasonIdToTeamsTable.putItem(seasonIdToTeam)
    }

    fun getTeamName(ncaaId: String): String {
        val queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(ncaaId).build())
        val pageResults: PageIterable<SeasonIdToTeams>  = seasonIdToTeamsTable.query(queryConditional)
        val teamName = pageResults.items().firstOrNull() ?: return ""
        return teamName.teamName
    }

    fun batchAddGameStats(gameStatsList: List<GameStats>) {
        val chunks = gameStatsList.chunked(25)
        for (chunk in chunks) {
            val batchJobBuilder = WriteBatch.builder(GameStats::class.java)
                .mappedTableResource(gameStatsTable)

            for (game in chunk) {
                batchJobBuilder.addPutItem(game)
            }
            val request =  BatchWriteItemEnhancedRequest.builder().addWriteBatch(batchJobBuilder.build()).build()
            val batchWriteResult = enhancedClient.batchWriteItem(request)
            val unprocessedItems: List<GameStats> = batchWriteResult.unprocessedPutItemsForTable(gameStatsTable)
            if (unprocessedItems.isNotEmpty()) {
                logger.warn("encountered ${unprocessedItems.size} unprocessed items")
            }
        }
    }

    fun addIdToTeamName(ncaaId: String, teamName: String) {
        val seasonIdToTeam = SeasonIdToTeams()
        seasonIdToTeam.setAllFields(ncaaId, teamName)
        seasonIdToTeamsTable.putItem(seasonIdToTeam)
    }

    fun processSeason(season: Season) {
        val dynamoSeason = TeamSeason(
            name = season.teamName,
            seasonYear = season.year,
            conference = season.conference,
            ncaaSeasonId = season.seasonId,
            coach = season.coach,
            contestIds = mutableListOf()
        )

        val contestIds = mutableListOf<String>()
        val gameStatsList = mutableListOf<GameStats>()

        for (game in season.games) {
            if (game.contestId.isEmpty()) {
                logger.warn("Skipping game with empty contest id: ${game.gameText}")
                continue
            }
            val gameStat = GameStats(
                game.contestId,
                season.year,
                game.homeTeamId,
                game.homeTeamName,
                game.awayTeamId,
                game.awayTeamName,
                game.homeScore,
                game.awayScore,
                game.date,
                game.gameText
            )
            gameStatsList.add(gameStat)
            contestIds.add(game.contestId)
        }
        dynamoSeason.contestIds = contestIds
        addTeamSeason(dynamoSeason)
        batchAddGameStats(gameStatsList)
    }
}