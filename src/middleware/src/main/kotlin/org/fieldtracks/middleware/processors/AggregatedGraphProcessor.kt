package org.fieldtracks.middleware.processors

import org.fieldtracks.middleware.AggregatedGraph
import org.fieldtracks.middleware.BatchProcessor
import org.fieldtracks.middleware.StoneReport
import org.slf4j.LoggerFactory

class AggregatedGraphProcessor: BatchProcessor<AggregatedGraph, StoneReport> {

    private val log = LoggerFactory.getLogger(AggregatedGraphProcessor::class.java)
    private var currentGraph: AggregatedGraph? = null

    override fun processBatch(prevResult: AggregatedGraph?, messages: List<StoneReport>): AggregatedGraph? {
       log.debug("Constructing graph")
        return null
    }


}
