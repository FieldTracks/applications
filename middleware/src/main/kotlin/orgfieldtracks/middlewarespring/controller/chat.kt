package orgfieldtracks.middlewarespring.controller

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import orgfieldtracks.middlewarespring.model.ChatMessage
import orgfieldtracks.middlewarespring.model.OutMessage
import java.text.SimpleDateFormat
import java.util.*

@Controller
class ChatController{

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    fun send(message: ChatMessage): OutMessage {
        val time = SimpleDateFormat("HH:mm").format(Date())
        return OutMessage(message.from, message.text, time)
    }
}