package org.fieldtracks.middleware.processors

import org.fieldtracks.middleware.StoneStatistics
import org.fieldtracks.middleware.VoidBatchProcessor
import org.slf4j.LoggerFactory

class StoneStatisticsProcessor: VoidBatchProcessor<StoneStatistics> {

    private val log = LoggerFactory.getLogger(StoneStatisticsProcessor::class.java)
    override fun processBatch(messages: List<StoneStatistics>) {
        log.info("Todo: Write to influxdb")
    }
}
