import io.grpc.Server
import io.grpc.ServerBuilder

private val logger = Logger {}

class SmartHomeServer(private val port: Int) {
    private val server: Server =
        ServerBuilder
            .forPort(port)
            .addService(DHTService())
            .addService(BulbService())
            .build()

    fun start() {
        server.start()
        logger.log { "Server started, listening on $port" }
        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.log { "*** shutting down gRPC server since JVM is shutting down" }
                this@SmartHomeServer.stop()
                logger.log { "*** server shut down" }
            }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = SmartHomeServer(port)
    server.start()
    server.blockUntilShutdown()
}