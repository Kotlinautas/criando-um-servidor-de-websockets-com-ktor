package karro

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*

var ruas = mutableListOf("Rua do K", "Rua Soninho de Março", "Rua Sem Perdões", "Rua Meia Idade", "Rua do Patocórnio", "Rua do Deninho")
var histórico = mutableListOf("")

fun main() {
    val client = HttpClient {
        install(WebSockets)
    }
    runBlocking {
        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {

            val iniciarReceberMensagens = launch { receberMensagens() }
            val iniciarEnviarRua = launch { enviarRua() }

            iniciarEnviarRua.join()
            iniciarReceberMensagens.cancelAndJoin()
        }
    }
    client.close()
    println("Connection closed. Goodbye!")
}

suspend fun DefaultClientWebSocketSession.receberMensagens() {
    try {
        for (mensagem in incoming) {
            mensagem as? Frame.Text ?: continue

            val mensagem = mensagem.readText().split(":")

            if (mensagem.size != 2) continue

            val rua = mensagem[0]
            val vindo = mensagem[1] == "vindo"

            if (vindo) ruas.remove(rua) else ruas.add(rua)
        }
    } catch (e: Exception) {
        println("Erro enquanto recebia" + e.localizedMessage)
    }
}

suspend fun DefaultClientWebSocketSession.enviarRua() {
    while (true) {
        var rua = ruas.random()

        while (histórico.last() == rua){
            rua = ruas.random()
        }

        histórico.add(rua)

        println("Enviando Rua: $rua")
        enviarMensagem("$rua:vindo")

        val tempoParaProximaRua = (5..15).shuffled().first()
        delay((tempoParaProximaRua * 1000).toLong())

        enviarMensagem("$rua:saindo")
    }
}

suspend fun DefaultClientWebSocketSession.enviarMensagem(mensagem: String){
    try {
        send(mensagem)
    } catch (e: Exception) {
        println("Erro enquanto enviava: " + e.localizedMessage)
        return
    }
}
