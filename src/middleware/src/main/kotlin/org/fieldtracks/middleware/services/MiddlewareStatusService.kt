package org.fieldtracks.middleware.services


import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.paho.client.mqttv3.IMqttClient

@Path("/status")
class MiddlewareStatusService(private val client: IMqttClient, private val authService: AuthService) {

    enum class Status {
        DISCONNECTED, INSTALLER, RUNNING
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun currentStatus(): Status {
        return if(!client.isConnected) {
            Status.DISCONNECTED
        } else if(authService.adminPasswordSet()) {
            Status.RUNNING
        } else {
            Status.INSTALLER
        }
    }

}
