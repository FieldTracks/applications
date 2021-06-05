package org.fieldtracks.middleware

// Stone Data
data class StoneReport(val timestamp: String, val mac: String, val comment: String, val interval: Double, val devices: ArrayList<StoneContact>)
data class StoneContact(val min: Double, val max: Double, val avg: Double, val mac: String, val network_id: String?, val beacon_id: String?)

// Aggregated graph data
data class AggregatedGraph(val timestamp: String, val nodes: ArrayList<AggregatedGraphNode>, val links: ArrayList<AggregatedGraphLink>)
data class AggregatedGraphNode(val id: String, val mac: String, val lastSeen: String, val localstone: Boolean, val beaconid: String?)
data class AggregatedGraphLink(val source: String, val target: String, val timestmp: String, val rssi: Double)
