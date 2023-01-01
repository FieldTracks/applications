package org.fieldtracks.http

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.eclipse.microprofile.reactive.messaging.Channel
import org.fieldtracks.middleware.model.ScanGraph
import org.fieldtracks.mqtt.ScanMqttServiceBase
import org.jboss.resteasy.reactive.RestStreamElementType
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType

@Path("aggregated/graph")
class AggregatedGraphHttpService {

    @Channel("aggregatedGraph")
    @Inject
    lateinit var graphChannel: Multi<ScanGraph>

    @Inject
    lateinit var graphService: ScanMqttServiceBase

    @GET
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    fun stream(): Multi<ScanGraph> {
        val cached = Multi.createFrom().item(graphService.lastGraph)
        return Multi.createBy().merging().streams(cached,graphChannel)
    }



}
