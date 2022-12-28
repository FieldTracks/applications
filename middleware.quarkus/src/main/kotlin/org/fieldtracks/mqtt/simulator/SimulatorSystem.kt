package org.fieldtracks.mqtt.simulator

import io.quarkus.arc.profile.IfBuildProfile
import org.fieldtracks.mqtt.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Named

class SimulatorSystem{


    @ApplicationScoped
    @IfBuildProfile("sim")
    @Named("scanMqttService")
    class Simulator: AbstractSimulatorMqttService(16,64)

    @ApplicationScoped
    @IfBuildProfile("sim")
    class IdentityResolver: NameResolver {
        override fun resolve(id: String) = id
    }

}

