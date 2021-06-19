package org.fieldtracks.middleware

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.fieldtracks.middleware.processors.AggregatedGraphProcessor
import org.fieldtracks.middleware.processors.StoneStatisticsProcessor
import org.fieldtracks.middleware.utils.AppConfiguration
import org.slf4j.LoggerFactory
import java.util.zip.Deflater

class MessageRouter(private val mqttClient: MqttAsyncClient, private val conf: AppConfiguration) {
    private val log = LoggerFactory.getLogger(MessageRouter::class.java)

    private val reportLoggingReceiver = SimpleReceiver(topic = "JellingStone/") { topic: String, message: String ->
        log.info("Received stone report", message)
    }

    private val graphReceiver = ScheduledBatchReceiver(
        name = "aggregated-graph",
        publisher = CompressingPublisher(mqttClient,true,0),
        processor = AggregatedGraphProcessor(conf),
        updateTopicStructure = "JellingStone/" to StoneReport::class.java,
        resultTopicStructure = "Aggregated/Graph" to AggregatedGraph::class.java,
        updateTopicPattern = "JellingStone/#",
        invokeIfNopdates = true
    )

    private val statisticsReceiver = ScheduledBatchReceiver(
        name = "stoneStatistics",
        processor = StoneStatisticsProcessor(),
        updateTopicStructure = "JellingStoneStatus/" to StoneStatistics::class.java,
        updateTopicPattern = "JellingStoneStatus/#",
        invokeIfNopdates = false
    )

    private val receivers = listOf(graphReceiver,reportLoggingReceiver,statisticsReceiver)

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
class CompressingPublisher(
    private val mqttClient: MqttAsyncClient,
    private val retain: Boolean,
    private val qos: Int
    ): Publisher {
    private val mapper = jacksonObjectMapper()

    override fun publish(topic: String, messageData: Any) {

        val data = mapper.writeValueAsBytes(messageData)
        val buffer = ByteArray(data.size)
        val deflate = Deflater(Deflater.BEST_COMPRESSION)
        deflate.setInput(data)
        deflate.finish()
        val length = deflate.deflate(buffer)
        deflate.end()
        val stripped = buffer.take(length).toByteArray()
        mqttClient.publish(topic,stripped,qos,retain)
    }

}

interface MessageReceiver {
    fun onMessageReceived(topic: String, message: String)
    fun mqttTopics(): List<String>
}
class SimpleReceiver(
    private val topic: String,
    private val receivedHandler: (topic: String, message: String) -> Unit): MessageReceiver {
    override fun onMessageReceived(topic: String, message: String) {
        receivedHandler.invoke(topic,message)
    }

    override fun mqttTopics(): List<String> {
        return listOf(topic)
    }

}

@FunctionalInterface
interface BatchProcessor<RESULT,UPDATE>{
    fun processBatch(prevResult: RESULT?, messages: List<UPDATE>): RESULT?
}

@FunctionalInterface
interface VoidBatchProcessor<UPDATE>: BatchProcessor<Void,UPDATE> {
    override fun processBatch(prevResult: Void?, messages: List<UPDATE>): Void? {
        processBatch(messages)
        return null
    }
    fun processBatch(messages: List<UPDATE>);
}

@FunctionalInterface
interface Publisher {
    fun publish(topic: String, messageData: Any)
}
