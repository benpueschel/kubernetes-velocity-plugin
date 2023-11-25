package net.kubecloud

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.ClientBuilder
import org.slf4j.Logger
import java.nio.file.Path


@Plugin(id = "kubecloud", name = "KubeCloud", version = "0.0.1")
class KubeCloud  {

    private val server: ProxyServer
    private val logger: Logger
    private val dataDirectory: Path
    private val api: CoreV1Api
    private val serviceManager: ServiceManager

    @Inject
    constructor(server: ProxyServer, logger: Logger, @DataDirectory dataDirectory: Path) {
        this.server = server
        this.logger = logger
        this.dataDirectory = dataDirectory;


        this.api = CoreV1Api(ClientBuilder.cluster().build())
        this.serviceManager = ServiceManager(this.api, ServiceEvent(this.server), this.logger);

        logger.info("GreyCloud initialized")
    }

    @Subscribe
    fun onProxyInitialization(@Suppress("UNUSED_PARAMETER") event: ProxyInitializeEvent) {
        this.serviceManager.isRunning = true
        this.serviceManager.start()
    }

    @Subscribe
    fun onProxyShutdown(@Suppress("UNUSED_PARAMETER") event: ProxyShutdownEvent) {
        println("Waiting for Service Manager to shut down...")
        this.serviceManager.isRunning = false
        while(this.serviceManager.isAlive) {
            Thread.sleep(20)
        }
        println("Service Manager shut down.")
    }

}