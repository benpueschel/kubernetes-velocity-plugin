package net.kubecloud

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.kubernetes.client.openapi.models.V1Pod
import java.net.InetSocketAddress

class ServiceEvent(private val server: ProxyServer) : ServiceEventInterface {

    private val gameServers = HashMap<String, ServerInfo>()

    private fun registerGameServer(pod: V1Pod): ServerInfo? {
        val metadata = pod.metadata
        if (metadata?.name == null)
            return null

        if(pod.status?.podIP == null)
            return null

        val serverInfo = ServerInfo(metadata.name, InetSocketAddress(pod.status?.podIP, 25565))
        if(gameServers[metadata.name!!] == serverInfo)
            return serverInfo // return if server is already registered under the same ip

        gameServers[metadata.name!!] = serverInfo
        server.registerServer(serverInfo)
        return serverInfo
    }

    private fun unregisterGameServer(pod: V1Pod): ServerInfo? {
        val metadata = pod.metadata
        if (metadata?.name == null)
            return null
        val serverInfo = gameServers[metadata.name!!]
        server.unregisterServer(serverInfo)
        gameServers.remove(metadata.name!!)
        return serverInfo
    }

    private fun registerServer(pod: V1Pod) {
        when (pod.metadata?.labels?.get("kind")) {
            "mc-server" -> {
                val serverInfo = registerGameServer(pod)
                if(serverInfo != null)
                    println("Registered Server ${serverInfo.name} (${serverInfo.address.hostName}:${serverInfo.address.port})")
            }
            "lobby-server" -> {
                val serverInfo = registerGameServer(pod)
                if(serverInfo != null) {
                    server.configuration.attemptConnectionOrder.add(serverInfo.name)
                    println("Registered Lobby Server ${serverInfo.name} (${serverInfo.address.hostName}:${serverInfo.address.port})")
                }
            }
            "proxy-server" -> {
                // TODO: sync up proxies, establish direct socket connection
            }
        }
    }

    private fun unregisterServer(pod: V1Pod) {
        when (pod.metadata?.labels?.get("kind")) {
            "mc-server" -> {
                val serverInfo = unregisterGameServer(pod)
                if(serverInfo != null)
                    println("Unregistered Server " +
                            "${serverInfo.name} (${serverInfo.address.hostName}:${serverInfo.address.port})")
            }
            "lobby-server" -> {
                val serverInfo = unregisterGameServer(pod)
                if(serverInfo != null) {
                    server.configuration.attemptConnectionOrder.remove(serverInfo.name)
                    println("Unregistered Lobby Server " +
                                "${serverInfo.name} (${serverInfo.address.hostName}:${serverInfo.address.port})")
                }
            }
            "proxy-server" -> {
            }
        }
    }

    override fun podCreated(pod: V1Pod) {
        registerServer(pod)
    }

    override fun podDeleted(pod: V1Pod) {
        unregisterServer(pod)
    }

    override fun podModified(pod: V1Pod) {

        when (pod.status?.phase) {
            "Pending" -> {
            }
            "Running" -> {
                registerServer(pod)
            }
            "Succeeded" -> {
                unregisterServer(pod)
            }
            "Failed" -> {
                unregisterServer(pod)
            }
        }

    }

}