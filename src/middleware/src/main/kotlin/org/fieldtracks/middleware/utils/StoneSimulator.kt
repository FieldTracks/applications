package org.fieldtracks.middleware.utils

import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.fieldtracks.middleware.CompressingPublisher
import org.fieldtracks.middleware.StoneContact
import org.fieldtracks.middleware.StoneReport
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer


class StoneSimulator(mqttAsyncClient: MqttAsyncClient, config: AppConfiguration, stonesNum: Int = 2, deviceNum: Int = 5) {
    private val log = LoggerFactory.getLogger(StoneSimulator::class.java)
    private val network = hToS(UUID.randomUUID().mostSignificantBits,10) // Eddystone: 10 byte namespace

    private val publisher = CompressingPublisher(mqttAsyncClient,false,0)

    private val stones = stonesNum.downTo(1).map {
        val mac = UUID.randomUUID().mostSignificantBits // Cut-off 48 bit
        SimulatedStone(
            mac = hToS(mac,6),
            comment = "Simulated Stone Number $it",
            interval = 8.0,
            instance = it
        )
    }

    private val devices = deviceNum.downTo(1).map {
        val mac = UUID.randomUUID().mostSignificantBits // Cut-off 48 bit
        val instance = hToS(UUID.randomUUID().leastSignificantBits,6) // Cut-off 48 bit
        val isBeacon = Math.random() < 0.1 // Beacon with ~ 10% propability
        SimulatedNonStoneDevice(
            mac = hToS(mac,6),
            network_id = if(isBeacon) { network } else { null },
            beacon_id = if(isBeacon) { instance } else { null }
        )
    }

    fun schedule() {
        fixedRateTimer("Stone Simulator", false, 8000, 8000) {
            log.info("Updating simulated Stones")
            publishStones()
        }
    }

    fun publishStones() {
        val tstmp = LocalDateTime.now().toString()
        val reports = stones.map {StoneReport(
            timestamp = tstmp,
            mac = it.mac,
            comment = it.comment,
            interval = 8.0,devices = ArrayList()
        )}
        // Assume 20% chance seeing a certain stone
        for(report in reports) {
            for(stone in stones) {
                if(Math.random() >= 0.8) {
                    val rssi = rndRssi()
                    report.devices.add(StoneContact(
                        min = rssi.first,
                        max = rssi.second,
                        avg = rssi.third,
                        mac = stone.mac,
                        network_id = network,
                        beacon_id = hToS(stone.instance.toLong(),10)
                    ))
                }
            }
        }
        // For non-Stone devices: At least one contact - again: Assume a 20% chance of seeing a certain stone
        for(device in devices) {
            val stone = reports[ThreadLocalRandom.current().nextInt(0,stones.size -1)]
            val sr = rndRssi()
            stone.devices.add(StoneContact(sr.first,sr.second,sr.third,device.mac,device.network_id,device.beacon_id))
            for(report in reports) {
                if(stone.mac != report.mac && Math.random() > 0.8) {
                    val rr = rndRssi()
                    report.devices.add(StoneContact(rr.first,rr.second,rr.third,device.mac,device.network_id,device.beacon_id))
                }
            }
        }

        // Ok, publish
        for(report in reports) {
            publisher.publish("JellingStone/${report.mac}",report)
        }
    }
    private fun hToS(num: Long, bytes: Int): String {
        val result = ArrayList<String>()
        var remainder = num
        bytes.downTo(1).forEach {
            result.add(0,String.format("%02X", Math.abs(remainder % 255)))
            remainder /= 255
        }
        return result.joinToString(separator = ":") { it }
    }

}
internal data class SimulatedStone(val mac: String, val comment: String, val interval: Double, val instance: Int)
data class SimulatedNonStoneDevice(val mac: String, val network_id: String?, val beacon_id: String?)


private fun rndRssi(): Triple<Double,Double,Double> {
    val first = ThreadLocalRandom.current().nextDouble(-80.0, -50.0)
    val second = ThreadLocalRandom.current().nextDouble(-80.0, -50.0)
    return Triple(minOf(first,second), maxOf(first,second), (first + second) / 2.0)

}
