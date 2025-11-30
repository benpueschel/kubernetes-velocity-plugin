package net.kubecloud

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.ConfigurationOptions
import ninja.leaping.configurate.SimpleConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.DumperOptions
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.notExists
import kotlin.io.path.pathString

class KubeCloudConfig(configPath: Path, logger: Logger) {

	private val rootNode: ConfigurationNode

	init {
		val configLoader = YAMLConfigurationLoader.builder()
			.setPath(configPath)
			.build()
		if (configPath.notExists()) {
			try {
				logger.info("config file does not exist. creating file...")
				configPath.createFile()
				val defaultRoot = configLoader.load()
				defaultRoot.getNode("kubernetes").act { kubernetes ->
					defaultNodeValue(
						kubernetes.getNode("namespace"),
						"minecraft",
						"The target namespace to watch for pod events"
					)
					defaultNodeValue(
						kubernetes.getNode("timeout"),
						"300",
						"The timeout in seconds after which a new watch will be created."
					)

					configLoader.save(defaultRoot)
				}
			} catch (exception: IOException) {
				logger.error("could not create default config: ${exception.message}")
				throw exception
			}
		}
		try {
			logger.info("loading config file.")
			rootNode = configLoader.load()
		} catch (exception: IOException) {
			logger.error("could not load config: ${exception.message}")
			throw exception
		}
	}

	private fun defaultNodeValue(node: ConfigurationNode, value: Any, comment: String?) {
		node.value = value
		if (comment == null)
			return

		// (node as CommentedConfigurationNode).setCommentIfAbsent(comment)
	}

	private fun getNode(path: String): ConfigurationNode {
		var node: ConfigurationNode = rootNode
		for (nodeName in path.split(".")) {
			node = node.getNode(nodeName)
		}
		return node
	}

	fun getString(path: String): String? = getNode(path).getString(null)

	fun getInt(path: String): Int = getNode(path).getInt(0)

	fun getFloat(path: String): Float = getNode(path).getFloat(0.0f)

	fun getDouble(path: String): Double = getNode(path).getDouble(0.0)

	fun getBoolean(path: String): Boolean = getNode(path).getBoolean(false)

	fun getStringList(path: String): List<String>? = getList(path, TypeToken.of(String::class.java))

	fun <T> getList(path: String, type: TypeToken<T>): List<T>? = getNode(path).getList(type, null)

	operator fun get(path: String): Any? = getNode(path).value

}
