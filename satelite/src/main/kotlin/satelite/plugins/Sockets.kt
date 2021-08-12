package satelite.plugins

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.Duration

data class Connection(val session: DefaultWebSocketSession)

fun Application.configureSockets() {
    install(WebSockets){
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = mutableListOf<Connection>()
        webSocket("/chat") {
            val thisConnection = Connection(this)
            connections.add(thisConnection)
            try {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val response = frame.readText()
                    connections.forEach {
                        if (it != thisConnection) it.session.send(response)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                connections.remove(thisConnection)
            }
        }
    }
}
