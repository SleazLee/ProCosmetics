package se.file14.procosmetics.gradle

import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

abstract class VersionedObfTask : DefaultTask() {

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:InputFile
    abstract val devBundleZip: RegularFileProperty

    @get:Internal
    protected val buildDir: File
        get() = project.layout.buildDirectory.get().asFile

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

fun ensurePaperDevBundleExtracted(buildDir: File, minecraftVersion: String, bundleFile: File): File {
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
