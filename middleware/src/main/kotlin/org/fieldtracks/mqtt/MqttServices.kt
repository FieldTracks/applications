package org.fieldtracks.mqtt

import io.quarkus.arc.DefaultBean
import io.quarkus.runtime.StartupEvent
import org.fieldtracks.FlushConfiguration
import org.fieldtracks.MiddlewareConfiguration
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.enterprise.inject.Produces
import javax.inject.Named
import kotlin.concurrent.thread


@ApplicationScoped
class MqttServices(
    val flushCfg: FlushConfiguration,
    final val cfg: MiddlewareConfiguration,
    val mqttConnector: MqttConnector,
    val nameService: NameMqttService,
    val authService: AuthMqttService,
    val stoneStatusService: StoneStatusMqttService,
    @Named("scanMqttService") val aggregatorService: ScanMqttServiceBase
) {


    @Produces
    @DefaultBean
    fun nameResolver(nameService: NameMqttService): NameResolver {
        return object: NameResolver {
            override fun resolve(id: String): String {
                return nameService.resolve(id)
            }
        }
    }

    fun onStart(@Observes event: StartupEvent) {
        val mqttServices = listOf(nameService,stoneStatusService,aggregatorService,authService)
        thread(start = true) {
            mqttConnector.connectBlocking(mqttServices)
        }
    }
}
