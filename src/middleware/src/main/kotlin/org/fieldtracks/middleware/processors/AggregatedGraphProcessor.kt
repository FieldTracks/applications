package org.fieldtracks.middleware.processors

import org.fieldtracks.middleware.*
import org.fieldtracks.middleware.utils.AppConfiguration
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

class AggregatedGraphProcessor(val conf: AppConfiguration): BatchProcessor<AggregatedGraph, StoneReport> {

    private val log = LoggerFactory.getLogger(AggregatedGraphProcessor::class.java)
    private var currentGraph: AggregatedGraph? = null

    override fun processBatch(prevResult: AggregatedGraph?, messages: List<StoneReport>): AggregatedGraph? {
        if(currentGraph == null && prevResult != null) {
            currentGraph = prevResult
        }
        val graphNodes = currentGraph?.nodes ?: emptyList()
        val graphLinks = currentGraph?.links ?: emptyList()

        val nodes = graphNodes.filter { valid(it) }.associateBy { it.id }.toMutableMap()
        val links = graphLinks.filter { valid(it) }.associateBy { it.source to it.target }.toMutableMap()
        for(message in messages) {
            val existingData = nodes[message.id()]
            if(existingData == null || existingData.lastSeen.isBefore(message.timestamp)){
                nodes[message.id()] = AggregatedGraphNode(id=message.id(), lastSeen = message.timestamp, true)
            }
            for(contact in message.data) {

                // Update node info
                val existingContactData = nodes[contact.id()]
                if(existingContactData == null || existingContactData.lastSeen.isBefore(message.timestamp)) {
                    val isStone = existingContactData != null && existingContactData.localstone
                    nodes[contact.id()] = AggregatedGraphNode(id=contact.id(),lastSeen = message.timestamp, localstone = isStone)
                }

                // Update link info
                val existingLink = links[message.id() to contact.id()]
                if(existingLink == null || existingLink.timestmp.isBefore(message.timestamp)) {
                    links[message.id() to contact.id()]=
                        AggregatedGraphLink(source = message.id(),target = contact.id(),message.timestamp,contact.avg)
                }
            }
        }
        currentGraph = AggregatedGraph(ZonedDateTime.now(), nodes.values.toList(), links.values.toList())

        return currentGraph
    }

    private fun StoneReport.id(): String {
       return "${conf.eddystoneNetworkId}-${mac}"
    }

    private fun StoneContact.id(): String {
        return if(network_id != null && beacon_id != null) {
            "${network_id}-${beacon_id}"
        } else {
            mac
        }
    }

    private fun valid(node: AggregatedGraphNode): Boolean {
        return node.lastSeen.plusHours(conf.contactTimeoutInHours).isAfter(ZonedDateTime.now())
    }

    private fun valid(link: AggregatedGraphLink): Boolean {
        return link.timestmp.plusHours(conf.contactTimeoutInHours).isAfter(ZonedDateTime.now())
    }
}
