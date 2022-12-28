package org.fieldtracks.mqtt

import org.fieldtracks.FlushConfiguration
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class NameMqttService: AbstractServiceBase() {

    @Inject
    protected lateinit var fCfg : FlushConfiguration

    private val reportQueue = LinkedBlockingQueue<NameUpdate>()

    override val schedule: Schedule = Schedule(8,1)



    override val flushTopics: List<String>
        get() = vT(fCfg.flushNames to "Names/aggregated")

    private val logger = LoggerFactory.getLogger(NameMqttService::class.java)


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
