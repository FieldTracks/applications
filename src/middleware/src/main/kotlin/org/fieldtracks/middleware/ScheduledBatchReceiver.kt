package org.fieldtracks.middleware

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.fixedRateTimer

class ScheduledBatchReceiver<RESULT,UPDATE>(
    private val name: String, // To be used in scheduling Thread
    private val invokeIfNopdates: Boolean, // Perfrom the invocation, even if there are no updates
    private val processor: BatchProcessor<RESULT,UPDATE>,
    private val updateTopicStructure: Pair<String, Class<UPDATE>>,
    private val resultTopicStructure: Pair<String, Class<RESULT>>? = null,
    private val publisher: Publisher? = null,
    private val updateTopicPattern: String = updateTopicStructure.first,
    private val maxBatchSize:Int = 100_000, // Upper limit for batch-size
): MessageReceiver {
    private val updateQueue = ConcurrentLinkedQueue<UPDATE>()
    private val prevResultQueue = ConcurrentLinkedQueue<RESULT>()
    private val log = LoggerFactory.getLogger(ScheduledBatchReceiver::class.java)
    private val mapper = jacksonObjectMapper()

    // To be called, when a new MQTT-Message is received
    override fun onMessageReceived(topic: String, message: String) {
        try {
            if(topic.startsWith(updateTopicStructure.first)){
                updateQueue.add(mapper.readValue(message,updateTopicStructure.second))
            } else if(resultTopicStructure != null && topic.startsWith(resultTopicStructure.first)) {
                prevResultQueue.add(mapper.readValue(message,resultTopicStructure.second))
            }
        } catch (e: Exception) {
            log.error("Skipping non-parseable message in topic $topic - content: $message", e)
            return
        }
    }

    fun schedule( delay: Long, interval: Long): ScheduledBatchReceiver<RESULT,UPDATE> {
        fixedRateTimer(name,false,delay,interval) {
            processBatch()
        }
        return this
    }

    override fun  mqttTopics(): List<String> {
        return listOf(updateTopicPattern,resultTopicStructure?.first ).mapNotNull { it }
    }


    internal fun processBatch() {
        val elements = ArrayList<UPDATE>()
        val receivedResults = ArrayList<RESULT>()
        var currentElement = updateQueue.poll()
        var cnt = 0
        while(currentElement != null && cnt < maxBatchSize) {
            cnt++
            elements += currentElement
            currentElement = updateQueue.poll()
        }
        if(cnt == maxBatchSize) {
            log.error("Received more than $maxBatchSize messages for $name. Possible message strom")
        }
        var receivedResult = prevResultQueue.poll()
        var resultCnt = 0
        while (receivedResult != null) {
            resultCnt++
            receivedResults += receivedResult
            receivedResult = prevResultQueue.poll()
        }
        if(resultCnt > 1) {
            log.error("Multiple aggregators for $name. - Received $resultCnt previous results while collecting reports")
        }
        val result = processor.processBatch(receivedResults.lastOrNull(),elements);
        if(invokeIfNopdates || cnt > 0) {
            if(publisher != null && resultTopicStructure != null && result != null ) {
                publisher.publish(resultTopicStructure.first,result)
            }
        }
    }

}

