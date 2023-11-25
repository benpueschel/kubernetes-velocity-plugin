package net.kubecloud

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.notExists


class KubeCloudConfig(val configPath: Path) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val rootNode: ConfigurationNode

    init {
        val configLoader = YAMLConfigurationLoader.builder()
                .setPath(configPath)
                .build()
        if(configPath.notExists()) {
            configPath.createFile()
            val defaultRoot = configLoader.load()
            val kubernetes = defaultRoot.getNode("kubernetes")
            kubernetes.getNode("namespace").value = "minecraft"
            configLoader.save(defaultRoot)
        }
        rootNode = configLoader.load()
    }

    private fun getNode(path: String): ConfigurationNode {
        var node: ConfigurationNode = rootNode
        for(nodeName in path.split(".")) {
            node = node.getNode(nodeName)
        }
        return node
    }

    fun getString(path: String): String? {
        return getNode(path).getString(null)
    }

    fun getInt(path: String): Int {
        return getNode(path).getInt(0)
    }

    fun getFloat(path: String): Float {
        return getNode(path).getFloat(0.0f)
    }

    fun getDouble(path: String): Double {
        return getNode(path).getDouble(0.0)
    }

    fun getBoolean(path: String): Boolean {
        return getNode(path).getBoolean(false)
    }

    fun getStringList(path: String): List<String>? {
        return getList(path, TypeToken.of(String::class.java))
    }

    fun <T> getList(path: String, type: TypeToken<T>): List<T>? {
        return getNode(path).getList(type, null)
    }

    fun get(path: String): Any? {
        return getNode(path).value
    }

}
