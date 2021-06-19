package org.fieldtracks.middleware.processors

import org.fieldtracks.middleware.AggregatedStones
import org.fieldtracks.middleware.BatchProcessor
import org.fieldtracks.middleware.StoneStatistics
import org.fieldtracks.middleware.utils.AppConfiguration
import java.time.ZonedDateTime

class AggregatedStoneProcessor(private val conf: AppConfiguration) : BatchProcessor<AggregatedStones, StoneStatistics> {

    private var lastStones: AggregatedStones? = null

    override fun processBatch(prevResult: AggregatedStones?, messages: List<StoneStatistics>): AggregatedStones? {
        val now = ZonedDateTime.now()
        if(lastStones == null && prevResult != null) {
            lastStones = prevResult
        }

        // Old data
        val data = lastStones?.stones?.
            filter {it.timestamp.plusHours(conf.contactTimeoutInHours).isAfter(now)}?.
            associateBy { it.mac }?.toMutableMap() ?: HashMap()

        // Group new data to avoid duplicates, use the last report
        messages
            .groupBy { it.mac }
            .mapNotNull { it.value.maxByOrNull { report -> report.timestamp }}
            .forEach { data[it.mac] = it }

        lastStones = AggregatedStones(stones = ArrayList(data.values))
        return lastStones
    }
}
