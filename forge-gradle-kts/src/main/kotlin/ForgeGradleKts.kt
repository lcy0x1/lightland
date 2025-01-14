@file:Suppress("unused")

import com.google.gson.Gson
import net.minecraftforge.gradle.common.util.RunConfig
import net.minecraftforge.gradle.userdev.DependencyManagementExtension
import net.minecraftforge.gradle.userdev.UserDevExtension
import org.gradle.api.*
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.internal.DefaultJavaLanguageVersion
import org.yaml.snakeyaml.Yaml
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*

class ForgeGradleKts : Plugin<Project> {
    companion object {
        val yaml = Yaml()
    }

    override fun apply(target: Project) {
        if (target.rootProject != target) {
            apply(target.rootProject)
        }
        listOf(
            "local.properties",
            "local.yml",
            "local.yaml",
            "gradle.yml",
            "gradle.yaml"
        ).forEach { propertiesFile ->
            when {
                propertiesFile.endsWith(".properties") -> loadProperties(target, propertiesFile)
                propertiesFile.endsWith(".yml") || propertiesFile.endsWith(".yaml") ->
                    loadYaml(target, propertiesFile)
            }
        }
        target.afterEvaluate { project ->
            try {
                (project.extensions.getByName("java") as org.gradle.api.plugins.JavaPluginExtension).apply {
                    toolchain { java ->
                        java.languageVersion.set(DefaultJavaLanguageVersion.of(8))
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
}

val gson = Gson()

fun loadProperties(target: Project, propertiesFile: String) = try {
    val properties = Properties()
    properties.load(target.file(propertiesFile).inputStream())
    properties.forEach { (k, v) ->
        setProperty(target, k.toString(), v)
    }
} catch (e: Exception) {
}

fun loadYaml(target: Project, propertiesFile: String) {
    try {
        ForgeGradleKts.yaml.load<Map<String, Any>>(target.file(propertiesFile).inputStream()).forEach { (k, v) ->
            put(target, k, v)
        }
    } catch (e: Exception) {
        if (e !is FileNotFoundException) {
            e.printStackTrace()
        }
    }
}

fun put(target: Project, key: String, value: Any?) {
    when (value) {
        null -> return
        is String, is Byte, is Short, is Int, is Long, is Float, is Double, is Char ->
            setProperty(target, key, value)
        else -> {
            setProperty(target, key, value)
            if (value is Map<*, *>) {
                value.forEach { (k, v) ->
                    put(target, "$key.$k", v)
                }
            }
        }
    }
}

fun setProperty(target: Project, key: String, value: Any) {
    target.ext.set(key, value)
    try {
        target.setProperty(key, value)
    } catch (e: Exception) {
    }
}


val Project.fg
    get() = extensions.getByType(DependencyManagementExtension::class.java)

val Project.jeiVersion get() = property("jei_version") as String
val Project.mcVersion get() = property("mc_version") as String
val Project.forgeVersion get() = property("forge_version") as String

private val Project.sourceSet get() = extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer
private fun RunConfig.init(project: Project?) {
    project ?: return
    workingDirectory(project.rootProject.file("run"))
    mods.run {
        create(project.name) {
            it.source(project.sourceSet.getByName("main"))
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun NamedDomainObjectContainer<RunConfig>.createClient(
    name: String = "client",
    project: Project? = null,
    configureAction: Action<RunConfig>? = null
) {
    create("client") {
        it.taskName = if (name.startsWith("run")) {
            name
        } else if (name.isEmpty()) {
            "runClient"
        } else {
            "run${name[0].toUpperCase()}${name.substring(1)}"
        }
        it.init(project)
        if (project != null) {
            val clientArgs = (project.rootProject.properties["forge.run.userProperties.client.args"] as? List<Any>
                ?: emptyList()) + (project.properties["forge.run.userProperties.client.args"] as? List<Any>
                ?: emptyList())

            if (clientArgs.isNotEmpty()) {
                println("client args: $clientArgs")
                it.args.addAll(clientArgs.map { it.toString() })
            }

            val clientEnvironment =
                (project.rootProject.properties["forge.run.userProperties.client.environment"] as? Map<String, Any>
                    ?: emptyMap()) + (project.properties["forge.run.userProperties.client.environment"] as? Map<String, Any>
                    ?: emptyMap())
            if (clientEnvironment.isNotEmpty()) {
                it.environment(it.environment + clientEnvironment.entries.associate {
                    it.key to gson.toJson(it.value)
                })
            }

            val clientProperties =
                (project.rootProject.properties["forge.run.userProperties.client.properties"] as? Map<String, Any>
                    ?: emptyMap()) + (project.properties["forge.run.userProperties.client.properties"] as? Map<String, Any>
                    ?: emptyMap())
            if (clientProperties.isNotEmpty()) {
                it.properties(it.properties + clientProperties.entries.associate {
                    it.key to gson.toJson(it.value)
                })
            }
        }
        configureAction?.execute(it)
    }
}

fun NamedDomainObjectContainer<RunConfig>.createServer(
    name: String = "server",
    project: Project? = null,
    configureAction: Action<RunConfig>? = null
) {
    create("server") {
        it.taskName = if (name.startsWith("run")) {
            name
        } else if (name.isEmpty()) {
            "runServer"
        } else {
            "run${name[0].toUpperCase()}${name.substring(1)}"
        }
        it.init(project)
        configureAction?.execute(it)
    }
}

fun NamedDomainObjectContainer<RunConfig>.createData(
    name: String = "data",
    project: Project? = null,
    configureAction: Action<RunConfig>? = null
) {
    create("data") {
        it.taskName = if (name.startsWith("run")) {
            name
        } else if (name.isEmpty()) {
            "runData"
        } else {
            "run${name[0].toUpperCase()}${name.substring(1)}"
        }
        it.init(project)
        configureAction?.execute(it)
    }
}

/**
 * 添加生成的resources文件夹
 */
fun Project.useGeneratedResources() {
    sourceSet.getByName("main").resources {
        it.srcDir("src/generated/resources")
        it.srcDir("${rootProject.projectDir.absolutePath}/src/main/resources")
    }
}

/**
 * 禁用混淆 jar
 */
fun Project.excludeReobfJar() {
    val notRebofJarTask = gradle.startParameter.taskNames.find { taskName ->
        "reobfJar" in taskName || "shadowJar" in taskName
    } == null
    tasks.whenTaskAdded {
        if ((it is net.minecraftforge.gradle.patcher.task.TaskReobfuscateJar || it.name == "reobfJar") && (notRebofJarTask || it.project != this)) {
            it.enabled = false
        }
    }
}

private fun DependencyHandler.add(
    configurationName: String,
    group: String,
    name: String,
    version: String? = null,
    configuration: String? = null,
    classifier: String? = null,
    ext: String? = null
): Dependency? = (create(
    mapOf(
        "group" to group,
        "name" to name,
        "version" to version,
        "configuration" to configuration,
        "classifier" to classifier,
        "ext" to ext
    ).filterValues { it != null }
) as ExternalModuleDependency).run {
    add(configurationName, this)
}

fun DependencyHandler.minecraft(
    group: String,
    name: String,
    version: String? = null,
    configuration: String? = null,
    classifier: String? = null,
    ext: String? = null
): Dependency? = add("minecraft", group, name, version, configuration, classifier, ext)

fun DependencyHandler.minecraft(dependencyNotation: Any): Dependency? =
    add("minecraft", dependencyNotation)

fun DependencyHandler.minecraft(project: Project): Dependency? =
    minecraft(group = "net.minecraftforge", name = "forge", version = "${project.mcVersion}-${project.forgeVersion}")

val DependencyHandler.junit: Dependency?
    get() = add("api", "junit", "junit", "4.13.2")

val DependencyHandler.core: Dependency?
    get() = add("api", project(mapOf("path" to ":lightland-core")))
val DependencyHandler.magic: Dependency?
    get() = add("api", project(mapOf("path" to ":lightland-magic")))
val DependencyHandler.quest: Dependency?
    get() = add("api", project(mapOf("path" to ":lightland-quest")))
val DependencyHandler.terrain: Dependency?
    get() = add("api", project(mapOf("path" to ":lightland-terrain")))
val DependencyHandler.lombok: Unit
    get() {
        val lombokDependency = "org.projectlombok:lombok:1.18.20"
        add("compileOnly", lombokDependency)
        add("annotationProcessor", lombokDependency)

        add("testCompileOnly", lombokDependency)
        add("testAnnotationProcessor", lombokDependency)
    }

val DependencyHandler.mixin: Unit
    get() {
        val dep = "org.spongepowered:mixin:0.8.2:processor";
        add("annotationProcessor", dep)
        add("testAnnotationProcessor", dep)
    }

/**
 * 在test以外的任务中关闭所有test任务运行
 * @param excludeCompileTest 是否禁用编译过程
 */
fun Project.disableTests(excludeCompileTest: Boolean = true) {
    if (gradle.startParameter.taskNames.find { taskName ->
            ":test" in taskName
        } == null) {
        tasks.run {
            disableTasks {
                yieldAll("test", "testClasses", "processTestResources")
                if (excludeCompileTest) yieldAll("compileTestJava", "compileTestKotlin")
            }
        }
    }
}

fun Project.disableTasks(taskNames: suspend SequenceScope<String>.() -> Unit) = disableTasks(sequence(taskNames))
fun Project.disableTasks(vararg taskNames: String) = disableTasks(taskNames.iterator())
fun Project.disableTasks(taskNames: Iterable<String>) = disableTasks(taskNames.iterator())
fun Project.disableTasks(taskNames: Sequence<String>) = disableTasks(taskNames.iterator())
fun Project.disableTasks(taskNames: Iterator<String>) {
    tasksByNames(taskNames) {
        enabled = false
    }
}

inline fun Project.tasksByNames(
    noinline taskNames: suspend SequenceScope<String>.() -> Unit,
    action: Task.() -> Unit
) = tasksByNames(sequence(taskNames), action)

inline fun Project.tasksByNames(vararg taskNames: String, action: Task.() -> Unit) =
    tasksByNames(taskNames.iterator(), action)

inline fun Project.tasksByNames(taskNames: Iterable<String>, action: Task.() -> Unit) =
    tasksByNames(taskNames.iterator(), action)

inline fun Project.tasksByNames(taskNames: Sequence<String>, action: Task.() -> Unit) =
    tasksByNames(taskNames.iterator(), action)

inline fun Project.tasksByNames(taskNames: Iterator<String>, action: Task.() -> Unit) {
    tasks.run {
        taskNames.forEach {
            try {
                action(getByName(it))
            } catch (e: Exception) {
            }
        }
    }
}

suspend fun <T> SequenceScope<T>.yieldAll(vararg elements: T) = yieldAll(elements.asList())

private inline fun <reified T : Any> Project.configure(noinline configuration: T.() -> Unit) {
    convention.findByType(T::class.java)?.let(configuration)
        ?: convention.findPlugin(T::class.java)?.let(configuration)
        ?: convention.configure(T::class.java, configuration)
}

fun Project.configureForge(configuration: UserDevExtension.() -> Unit) {
    configure<UserDevExtension> {
        mappings("official", "1.16.5")
        configuration()
    }
}

fun Project.jar(configuration: Jar.() -> Unit) {
    tasks.getByName("jar") {
        configuration(it as Jar)
    }
}

fun Jar.defaultManifest(project: Project) {
    manifest {
        @Suppress("SpellCheckingInspection")
        it.attributes(
            it.attributes + mapOf(
                "Specification-Title" to "lightland-quest",
                "Specification-Vendor" to "hikarishima",
                "Specification-Version" to "1", // We are version 1 of ourselves
                "Implementation-Title" to project.name,
                "Implementation-Version" to archiveVersion.get(),
                "Implementation-Vendor" to "hikarishima",
                "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
            )
        )
    }
}