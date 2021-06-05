package org.fieldtracks.middleware

import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.fieldtracks.middleware.processors.AggregatedGraphProcessor
import org.slf4j.LoggerFactory

class MessageRouter(private val mqttClient: MqttAsyncClient) {
    private val log = LoggerFactory.getLogger(MessageRouter::class.java)

    private val reportLoggingReceiver = object : MessageReciver {
        val log = LoggerFactory.getLogger(this.javaClass)
        override fun onMessageReceived(topic: String, message: String) {
            if(topic.startsWith("JellingStone/")) {
                log.info("Received stone report", message)
            }
        }
        override fun mqttTopics() = listOf("JellingStone/#")
    }

    private val graphReceiver = ScheduledBatchReceiver(mqttClient = mqttClient,
        name = "aggregated-graph",
        processor = AggregatedGraphProcessor(),
        updateTopicStructure = "JellingStone/" to StoneReport::class.java,
        resultTopicStructure = "Aggregated/Graph" to AggregatedGraph::class.java,
        updateTopicPattern = "JellingStone/#")


    private val receivers = listOf(graphReceiver,reportLoggingReceiver)

    fun subscribe() {
        val topics = receivers.map { r -> r.mqttTopics()}.flatten().distinct().toTypedArray()
        log.debug("Subscribing to {}",topics)
        mqttClient.subscribe(topics, IntArray(topics.size){0})
        receivers.filterIsInstance<ScheduledBatchReceiver<*, *>>().forEach { it.schedule(8*1000,8*1000) }
    }

    fun process(topic: String, message: String) {
        log.debug("Processing message in $topic", message)
        receivers.forEach {it.onMessageReceived(topic,message)}
    }

}
