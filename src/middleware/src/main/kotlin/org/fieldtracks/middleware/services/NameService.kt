package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class NameService(client: IMqttClient,flushNames: Boolean)
    :ServiceBase(client,Schedule(8,1), vT(flushNames to "Names/aggregated")) {

    private val reportQueue = LinkedBlockingQueue<NameUpdate>()
    private val logger = LoggerFactory.getLogger(NameService::class.java)


    private val currentNameMap = AggregatedNames(ConcurrentHashMap())

    private val aggregatedTopic = "Names/aggregated"

    override fun onTimerTriggered() {
        val elements = mutableListOf(reportQueue.take()) // Blocking wait until one message arrives
        if(!reportQueue.isEmpty()) {
            elements.addAll(reportQueue.toList())
            reportQueue.removeAll(elements.toSet())
        }
        var changes = false

        elements.forEach {
            if(currentNameMap.nameById[it.id] != it.name) {
                currentNameMap.nameById[it.id] = it.name
                logger.info("Renaming: '{}'",it)
                changes = true
            }
        }
        if(changes) {
            publishCurrentMap()
        }
    }

    override fun onMqttConnectedInitially() {
        subscribeJSONMqtt("Names/updates") { _: String, msg: NameUpdate ->
            reportQueue.add(msg)
        }
        subscribeJSONMqtt(aggregatedTopic) { _, data: AggregatedNames ->
            currentNameMap.nameById.putAll(data.nameById)
        }
    }

    private fun publishCurrentMap() {
        publishMQTTJson(aggregatedTopic, currentNameMap)
    }

    fun resolve(id: String): String {
        return currentNameMap.nameById[id] ?: id
    }
}



data class NameUpdate(val id: String, val name: String?)

data class AggregatedNames(val nameById: ConcurrentHashMap<String, String?>)
