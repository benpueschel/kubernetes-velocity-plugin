package net.kubecloud

import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.Config
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory

fun main() {

    val client = Config.defaultClient()

    val interceptor = HttpLoggingInterceptor { message: String? -> println(message) }
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
    val api = CoreV1Api(client)

    val logger = LoggerFactory.getLogger("Main")

    val serviceManager = ServiceManager(api, object: ServiceEventInterface { }, logger)
    serviceManager.isRunning = true
    serviceManager.start()

    while(true) {
        Thread.sleep(200);
    }

}
