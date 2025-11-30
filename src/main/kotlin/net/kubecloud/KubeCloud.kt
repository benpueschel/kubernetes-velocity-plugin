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
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists

@Plugin(id = "kubecloud", name = "KubeCloud", version = "0.0.1")
class KubeCloud {

	private val server: ProxyServer
	private val logger: Logger
	private val dataDirectory: Path
	private val api: CoreV1Api
	private val serviceManager: ServiceManager
	private val config: KubeCloudConfig

	@Inject
	constructor(server: ProxyServer, logger: Logger, @DataDirectory dataDirectory: Path) {
		this.server = server
		this.logger = logger
		this.dataDirectory = dataDirectory;
		if (dataDirectory.notExists())
			dataDirectory.createDirectory()

		val configPath = File(this.dataDirectory.toFile(), "config.yml").toPath()
		this.config = KubeCloudConfig(configPath, this.logger)

		this.api = CoreV1Api(ClientBuilder.cluster().build())
		val serviceEventHandler = ServiceEvent(this.server, this.logger)
		this.serviceManager = ServiceManager(this.api, serviceEventHandler, this.logger, this.config);

		logger.info("GreyCloud initialized")
	}

	@Subscribe
	fun onProxyInitialization(@Suppress("UNUSED_PARAMETER") event: ProxyInitializeEvent) {
		this.serviceManager.isRunning = true
		this.serviceManager.start()
	}

	@Subscribe
	fun onProxyShutdown(@Suppress("UNUSED_PARAMETER") event: ProxyShutdownEvent) {
		logger.info("Waiting for Service Manager to shut down...")
		this.serviceManager.isRunning = false
		var threadState: Thread.State? = null
		while (this.serviceManager.isAlive) {
			if (threadState != this.serviceManager.state) {
				threadState = this.serviceManager.state
				logger.trace("Service Manager thread state: $threadState")
			}
			Thread.sleep(20)
		}
		logger.info("Service Manager shut down.")
	}
}
