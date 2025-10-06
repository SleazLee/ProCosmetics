import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.provider.Property
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://repo.papermc.io/repository/maven-public/")
        maven(url = "https://maven.fabricmc.net/")
    }
    dependencies {
        classpath("net.fabricmc:tiny-remapper:0.12.0")
    }
}

plugins {
    java
    id("com.gradleup.shadow") version "8.3.8"
}

group = "se.file14"
version = "2.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val projectVersion = project.version
    inputs.property("version", projectVersion)
    filesMatching("**/plugin.yml") {
        filter<ReplaceTokens>("tokens" to mapOf("VERSION" to projectVersion.toString()))
    }
}

// Configure shadow jar
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")

    // Exclude unnecessary files
    exclude("META-INF/maven/**")
    exclude("META-INF/versions/**")
    exclude("module-info.class")
    relocate("org.bstats", "se.file14.procosmetics.bstats")
}

// Make build depend on shadowJar instead of jar
tasks.named("build") {
    dependsOn("shadowJar")
}

// Disable the default jar task since we're using shadowJar
tasks.named("jar") {
    enabled = false
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        metadataSources {
            artifact()
        }
    }
    maven(url = "https://jitpack.io")
}

dependencies {
    subprojects.forEach {
        implementation(project(it.path))
    }
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenLocal()
        mavenCentral()

        // Paper
        maven(url = "https://repo.papermc.io/repository/maven-public/") {
            metadataSources {
                artifact()
            }
        }

        // Plugins
        maven(url = "https://jitpack.io")
        maven(url = "https://repo.essentialsx.net/releases/")
        maven(url = "https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven(url = "https://maven.enginehub.org/repo/")
        maven(url = "https://ci.ender.zone/plugin/repository/everything/")
        maven(url = "https://repo.rosewooddev.io/repository/public/")
    }
    val javaVersion = findProperty("javaVersion")?.toString()?.toIntOrNull() ?: 21

    tasks.withType<JavaCompile>().configureEach {
        javaCompiler.set(javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        })
    }

    if (extensions.extraProperties.has("remapMinecraftVersion")) {
        val mcVersion = extensions.extraProperties["remapMinecraftVersion"].toString()
        val paperDevBundle by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        dependencies {
            add(paperDevBundle.name, "io.papermc.paper:dev-bundle:$mcVersion-R0.1-SNAPSHOT")
        }

        val mojangJarProvider = paperDevBundle.elements.map { elements ->
            require(elements.size == 1) {
                "Expected exactly one Paper dev bundle for project ${name}"
            }
            val bundleFile = elements.single().asFile
            val extractedDir = ensurePaperDevBundleExtracted(layout.buildDirectory.get().asFile, mcVersion, bundleFile)
            extractedDir.resolve("data/paperclip-mojang.jar")
        }

        dependencies {
            add("compileOnly", project.files(mojangJarProvider))
        }

        extensions.extraProperties["paperDevBundleConfig"] = paperDevBundle
    }

}

// Obfuscation Tasks
abstract class VersionedObfTask : DefaultTask() {

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:Internal
    val buildDir: File = project.layout.buildDirectory.get().asFile

    @get:InputFile
    abstract val devBundleZip: RegularFileProperty

    init {
        group = "jar preparation"
        description = "Generates an obfuscated version of the jar to use with Paper!"
        dependsOn("shadowJar")
    }

    @TaskAction
    fun obfuscate() {
        val libsDir = "$buildDir/libs"
        val projectName = project.name
        val projectVersion = project.version.toString()
        val inputJar = Path.of(libsDir, "$projectName-$projectVersion.jar")
        val obfJar = Path.of(libsDir, "$projectName-$projectVersion-obf.jar")

        val extractedBundleDir = ensurePaperDevBundleExtracted(buildDir, minecraftVersion.get(), devBundleZip.asFile.get())
        val mappingFile = extractedBundleDir.resolve("data/mojang-spigot-reobf.tiny")
        val mojangJar = extractedBundleDir.resolve("data/paperclip-mojang.jar")

        require(mappingFile.exists()) {
            "Could not find Paper mapping file at ${mappingFile.absolutePath}"
        }
        require(mojangJar.exists()) {
            "Could not find Paper mojang-mapped jar at ${mojangJar.absolutePath}"
        }

        Files.deleteIfExists(obfJar)

        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappingFile.toPath(), "mojang", "spigot"))
            .ignoreConflicts(true)
            .rebuildSourceFilenames(true)
            .build()

        try {
            remapper.readClassPath(mojangJar.toPath())
            remapper.readInputs(inputJar)

            OutputConsumerPath.Builder(obfJar).build().use { outputConsumer ->
                outputConsumer.addNonClassFiles(inputJar, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.apply(outputConsumer)
            }
        } finally {
            remapper.finish()
        }

        Files.move(obfJar, inputJar, StandardCopyOption.REPLACE_EXISTING)
    }

}

private fun ensurePaperDevBundleExtracted(buildDir: File, minecraftVersion: String, bundleFile: File): File {
    val targetDir = File(buildDir, "paper-dev-bundles/$minecraftVersion")
    val mappingFile = File(targetDir, "data/mojang-spigot-reobf.tiny")

    if (mappingFile.exists() && mappingFile.lastModified() >= bundleFile.lastModified()) {
        return targetDir
    }

    if (targetDir.exists()) {
        targetDir.deleteRecursively()
    }
    targetDir.mkdirs()

    ZipInputStream(BufferedInputStream(bundleFile.inputStream())).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && (entry.name == "data/mojang-spigot-reobf.tiny" || entry.name == "data/paperclip-mojang.jar")) {
                val outputFile = File(targetDir, entry.name)
                outputFile.parentFile?.mkdirs()
                outputFile.outputStream().use { out ->
                    zip.copyTo(out)
                }
            }
            entry = zip.nextEntry
        }
    }

    return targetDir
}

subprojects.forEach { subproject ->
    if (subproject.extensions.extraProperties.has("paperDevBundleConfig") &&
        subproject.extensions.extraProperties.has("remapMinecraftVersion")) {
        val devBundleConfig = subproject.extensions.extraProperties["paperDevBundleConfig"] as Configuration
        val devBundleFileProvider = devBundleConfig.elements.map { elements ->
            require(elements.size == 1) {
                "Expected exactly one Paper dev bundle for project ${subproject.name}"
            }
            project.objects.fileProperty().fileValue(elements.single().asFile).get()
        }

        tasks.register<VersionedObfTask>("obf${subproject.name.replaceFirstChar { char -> char.uppercaseChar() }}") {
            dependsOn("shadowJar")
            minecraftVersion.set(subproject.extensions.extraProperties["remapMinecraftVersion"].toString())
            devBundleZip.set(devBundleFileProvider)
        }
    }
}

tasks.register("obfuscate") {
    dependsOn(tasks.withType<VersionedObfTask>())
    group = "jar preparation"
}

tasks.register("copyJarToPluginsFolder") {
    val folderPath = providers.environmentVariable("JarFolderPath")
    val projectName = project.name
    val projectVersion = project.version.toString()

    onlyIf { folderPath.isPresent }

    dependsOn("obfuscate")

    doLast {
        copy {
            from("build/libs/$projectName-$projectVersion.jar")
            into(folderPath.get())
            rename { it.replace("-$projectVersion", "") }
        }
    }
}