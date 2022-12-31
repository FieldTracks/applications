package org.fieldtracks.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.jwt.auth.principal.JWTParser
import io.smallrye.jwt.auth.principal.ParseException
import io.vertx.core.impl.ConcurrentHashSet
import org.fieldtracks.middleware.model.ScanGraph
import org.fieldtracks.mqtt.ScanMqttServiceBase
import org.slf4j.LoggerFactory
import java.util.Base64
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.websocket.*
import javax.websocket.CloseReason.CloseCode
import javax.websocket.CloseReason.CloseCodes
import javax.websocket.server.ServerEndpoint


@ServerEndpoint("/api/ws/graph")
@ApplicationScoped
class ScanGraphHttpService {

    @Inject
    protected lateinit var parser: JWTParser

    @Inject
    protected lateinit var mapper: ObjectMapper

    @Inject
    protected lateinit var scanMqttService: ScanMqttServiceBase

    @PostConstruct
    fun registerListener() {
        scanMqttService.aggregatedGraphListeners.add {
            onReportReceived(it)
        }
    }


    val sessions = ConcurrentHashSet<Session>()
    val logger = LoggerFactory.getLogger(ScanGraphHttpService::class.java)

    @OnOpen
    fun onOpen(session: Session) {
        logger.debug("Adding session {}", session)

    }

    @OnMessage
    fun onMessage(message: String, session: Session) {
        try {
            parser.parse(message)
            sendScanGraph(session, scanMqttService.lastGraph)
            sessions.add(session)
        } catch (e: ParseException) {
            val reason = CloseReason(CloseCodes.CANNOT_ACCEPT, "Invalid credentials")
            session.close(reason)
        }
    }

    @OnClose
    fun onClose(session: Session) {
        logger.debug("Removing session {}", session)
        sessions.remove(session)
    }

    @OnError
    fun onError(session: Session, throwable: Throwable) {
        logger.warn("Error in  session {} - removing", session, throwable)
        sessions.remove(session)
    }

    fun onReportReceived(graph: ScanGraph) {
        sessions.forEach { sendScanGraph(it,graph)}
    }

    private fun sendScanGraph(session: Session, graph: ScanGraph) {
        val data = mapper.writeValueAsString(graph)
        session.asyncRemote.sendText(data) {
            if (it.exception != null) {
                logger.warn("Error in session {} when sending graph {}",session,graph,it.exception)
            } else {
                logger.debug("Sent graph to {}", session)
            }
        }
    }

}
