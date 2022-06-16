package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.createObjectMapper
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class NameService(
    private val client: IMqttClient
):ScheduledServiceBase(initialDelaySeconds = 8, intervalSeconds = 1) {

    private val mapper = createObjectMapper()
    private val reportQueue = LinkedBlockingQueue<NameUpdate>()
    private val logger = LoggerFactory.getLogger(NameService::class.java)

    @Volatile
    private var currentNameMap = AggregatedNames(ConcurrentHashMap())

    override fun onTimerTriggered() {
        val elements = mutableListOf(reportQueue.take()) // Blocking wait until one message arrives
        if(!reportQueue.isEmpty()) {
            elements.addAll(reportQueue.toList())
            reportQueue.removeAll(elements.toSet())
        }
        var changes = false

        elements.forEach {
            if(currentNameMap.nameById[it.id!!] != it.name) {
                currentNameMap.nameById[it.id] = it.name
                logger.info("Renaming: '{}'",it)
                changes = true
            }
        }
        if(changes) {
            client.publish("names/aggregated",mapper.writeValueAsBytes(currentNameMap),1,true)
        }
    }

    override fun onMqttConnected(reconnect: Boolean) {
        if(!reconnect) {
            client.subscribe("names/updates") { _, data ->
                if(data == null ) {
                    logger.warn("Ignoring empty update message")
                } else {
                    try {
                        val updateMsg = mapper.readValue(data.payload,NameUpdate::class.java)
                        if(updateMsg.id == null || updateMsg.id.isBlank()) {
                            logger.warn("Update-Message has empty id - ignoring - data: '{}'",updateMsg)
                        } else {
                            reportQueue.add(updateMsg)
                        }
                    } catch (e: Exception) {
                        logger.warn("Error applying name-update '{}'", data,e)
                    }
                }
            }
            client.subscribe("names/aggregated") {_, data ->
                if(data == null ) {
                    logger.warn("Ignoring empty aggregation message")
                } else {
                    try {
                        currentNameMap = mapper.readValue(data.payload, AggregatedNames::class.java)
                    } catch (e: Exception) {
                        logger.warn("Error parsing aggregated names '{}'", data,e)
                    }
                }
            }
        }
    }


}

data class NameUpdate(val id: String?, val name: String?)

data class AggregatedNames(val nameById: ConcurrentHashMap<String, String?>)