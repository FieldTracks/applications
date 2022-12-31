package org.fieldtracks.http


import org.fieldtracks.mqtt.MqttServices
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/status")
class MiddlewareStatusService() {

    @Inject
    lateinit var mqttServices: MqttServices

    enum class Status {
        DISCONNECTED, INSTALLER, RUNNING
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun currentStatus(): MiddlewareStatus {
        return if(!mqttServices.mqttConnector.mqttClient.isConnected) {
            MiddlewareStatus(Status.DISCONNECTED)
        } else if(mqttServices.authService.adminPasswordSet()) {
            MiddlewareStatus(Status.RUNNING)
        } else {
            MiddlewareStatus(Status.INSTALLER)
        }
    }

    data class MiddlewareStatus(
        val status:Status
    )

}
