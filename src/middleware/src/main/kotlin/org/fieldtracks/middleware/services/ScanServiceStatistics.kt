package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.slf4j.LoggerFactory

class ScanServiceStatistics(client: IMqttClient) {

    private val logger = LoggerFactory.getLogger(ScanServiceStatistics::class.java)

    fun update(data: Set<ScanReportMessage>) {
        logger.debug("updating stats")
    }
}