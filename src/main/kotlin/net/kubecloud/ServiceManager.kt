package net.kubecloud

import com.google.gson.reflect.TypeToken
import io.kubernetes.client.custom.V1Patch
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.util.Watch
import org.slf4j.Logger
import java.net.InetAddress

class ServiceManager(
	private val api: CoreV1Api,
	private val callback: ServiceEventInterface,
	private val logger: Logger,
	config: KubeCloudConfig
) : Thread() {

	private var lastExecution: Long = 0
	private val timeoutSeconds: Int = config.getInt("kubernetes.timeout")
	private val namespace: String = config.getString("kubernetes.namespace") ?: "minecraft"

	var isRunning = false
	var revalidateServers = false

	private fun createWatch(resourceVersion: String?): Watch<V1Pod> {
		val call =
			api.listNamespacedPod(namespace)
				.watch(true)
				.resourceVersion(resourceVersion)
				.timeoutSeconds(timeoutSeconds)
				.executeAsync(null)
		lastExecution = System.currentTimeMillis()

		/*
        // JSON Patch that removes the first finalizer (index 0)
        val patch = V1Patch("[{\"op\": \"remove\", \"path\": \"/metadata/finalizers/0\"}]")

        // Kubernetes stores the pod's name in the container's hostname
        val podName = InetAddress.getLocalHost().hostName
        api.patchNamespacedPod(podName, namespace, patch).executeAsync(null)
		*/

		return Watch.createWatch(api.apiClient, call, object : TypeToken<Watch.Response<V1Pod?>?>() {}.type)
	}

	override fun run() {
		var resourceVersion: String? = null
		while (isRunning) {
			logger.info("Creating new watch...")

			try {
				createWatch(resourceVersion).use { watch ->
					while (!revalidateServers && System.currentTimeMillis() < lastExecution + timeoutSeconds * 1000) {
						for (event in watch) {
							val pod = event.`object`
							val metadata = pod.metadata

							resourceVersion = metadata?.resourceVersion

							logger.trace("Event type:    ${event.type}")
							logger.trace("Pod:           ${metadata?.name} (${pod.status?.podIP})")
							logger.trace("Pod status:    ${pod.status?.phase}")
							logger.trace("Pod reason:    ${pod.status?.reason}")
							logger.trace("Pod kind:      ${metadata?.labels?.get("kind")}")

							when (event.type) {
								"ADDED" -> callback.podCreated(pod)
								"MODIFIED" -> callback.podModified(pod)
								"DELETED" -> callback.podDeleted(pod)
							}

                            if (!isRunning) break
						}
						sleep(10)
						if (!isRunning) break
					}
				}
			} catch (e: ApiException) {
				logger.error("${e.code}: ${e.responseBody}");
				e.printStackTrace();
			}
		}
	}

}
