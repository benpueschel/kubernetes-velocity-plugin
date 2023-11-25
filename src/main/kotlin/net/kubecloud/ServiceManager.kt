package net.kubecloud

import com.google.gson.reflect.TypeToken
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.util.Watch
import org.slf4j.Logger

class ServiceManager(private val api: CoreV1Api, private val callback: ServiceEventInterface, private val logger: Logger) : Thread() {

    private var lastExecution: Long = 0
    private val timeoutSeconds = 5 * 60

    var isRunning = false
    var revalidateServers = false

    private fun createWatch(resourceVersion: String?): Watch<V1Pod> {
        val call = api.listNamespacedPodCall("minecraft",
            null, null, null, null, null,
            null, resourceVersion, null, null, timeoutSeconds, true, null)

        lastExecution = System.currentTimeMillis()
        return Watch.createWatch(api.apiClient, call, object : TypeToken<Watch.Response<V1Pod?>?>() {}.type)
    }

    override fun run() {
        var resourceVersion: String? = null
        while(isRunning) {
            logger.info("Creating new watch...")

            try {
                val watch = createWatch(resourceVersion)
                watch.use { watch ->
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
                        }
                        sleep(10)
                        if (!isRunning) break
                    }
                }
            } catch(e: ApiException) {
                logger.error("${e.code}: ${e.responseBody}");
                e.printStackTrace();
            }
        }
    }

}