package org.fieldtracks.middleware

import java.time.ZonedDateTime

// Stone Data
data class StoneReport(
    val timestamp: ZonedDateTime,
    val mac: String,
    val comment: String,
    val interval: Double,
    val data: ArrayList<StoneContact>,
)
data class StoneContact(val min: Double, val max: Double, val avg: Double, val mac: String, val network_id: String?, val beacon_id: String?)

// Aggregated graph data
data class AggregatedGraph(val timestamp: ZonedDateTime, val nodes: List<AggregatedGraphNode>, val links: List<AggregatedGraphLink>)
data class AggregatedGraphNode(val id: String, val lastSeen: ZonedDateTime, val localstone: Boolean)
data class AggregatedGraphLink(val source: String, val target: String, val timestmp: ZonedDateTime, val rssi: Double)

// TODO: Major-Minor -> Network, Instance
data class StoneStatistics(val mac: String,
                           val net: String,
                           val timestamp: ZonedDateTime,
                           val ver: String,
                           val int: Double, val up: Double,
                           val bat: Double, val bs: String,
                           val ch: Number, val rx: Number)

data class AggregatedStones(val stones: ArrayList<StoneStatistics>)


// Stone Names
data class AggregatedNames(val names: ArrayList<StoneName>)
data class StoneName(val id: String, val name: String)
