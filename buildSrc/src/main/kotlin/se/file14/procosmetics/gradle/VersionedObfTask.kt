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
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipInputStream

abstract class VersionedObfTask : DefaultTask() {

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:InputFile
    abstract val devBundleZip: RegularFileProperty

    @get:Internal
    protected val buildDirFile: File
        get() = project.layout.buildDirectory.get().asFile

    @get:Internal
    val bundleCacheDir: File
        get() = project.rootProject.layout.projectDirectory.dir(".gradle/paper-dev-bundles").asFile

    init {
        group = "jar preparation"
        description = "Generates an obfuscated version of the jar to use with Paper!"
        dependsOn("shadowJar")
    }

    @TaskAction
    fun obfuscate() {
        val libsDir = buildDirFile.toPath().resolve("libs")
        val projectName = project.name
        val projectVersion = project.version.toString()
        val inputJar = libsDir.resolve("$projectName-$projectVersion.jar")
        val obfJar = libsDir.resolve("$projectName-$projectVersion-obf.jar")

        val extractedBundleDir = ensurePaperDevBundleExtracted(bundleCacheDir, minecraftVersion.get(), devBundleZip.asFile.get())
        val mappingFile = extractedBundleDir.resolve("data/mojang-spigot-reobf.tiny")
        val resources = ensurePaperDevBundleResources(extractedBundleDir, minecraftVersion.get())
        val mojangJar = resources.mojangJar

        require(mappingFile.exists()) {
            "Could not find Paper mapping file at ${mappingFile.absolutePath}"
        }
        require(mojangJar.exists()) {
            "Could not find Paper mojang-mapped jar at ${mojangJar.absolutePath}"
        }

        Files.deleteIfExists(obfJar)

        val sourceNamespace = detectSourceNamespace(mappingFile)

        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappingFile.toPath(), sourceNamespace, "spigot"))
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

fun ensurePaperDevBundleExtracted(cacheRoot: File, minecraftVersion: String, bundleFile: File): File {
    val targetDir = File(cacheRoot, minecraftVersion)
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
            if (!entry.isDirectory) {
                val targetName = when {
                    entry.name == "data/mojang-spigot-reobf.tiny" || entry.name == "data/mojang+yarn-spigot-reobf.tiny" -> "data/mojang-spigot-reobf.tiny"
                    entry.name == "data/paperclip-mojang.jar" || entry.name == "data/paperclip-mojang+yarn.jar" -> "data/paperclip-mojang.jar"
                    else -> null
                }

                if (targetName != null) {
                    val outputFile = File(targetDir, targetName)
                    outputFile.parentFile?.mkdirs()
                    outputFile.outputStream().use { out ->
                        zip.copyTo(out)
                    }
                }
            }
            entry = zip.nextEntry
        }
    }

    return targetDir
}

data class PaperclipResources(val mojangJar: File, val patchedJar: File)

fun ensurePaperDevBundleResources(bundleDir: File, minecraftVersion: String): PaperclipResources {
    val paperclipJar = File(bundleDir, "data/paperclip-mojang.jar")
    require(paperclipJar.exists()) {
        "Could not find Paperclip jar at ${paperclipJar.absolutePath}"
    }

    val downloadContexts = readDownloadContexts(paperclipJar)
    val cacheDir = File(bundleDir, "cache")
    cacheDir.mkdirs()

    var mojangJar: File? = null

    downloadContexts.forEach { context ->
        val target = File(cacheDir, context.fileName)
        ensureFileWithHash(context.url, context.sha256, target)
        if (context.fileName.startsWith("mojang_")) {
            mojangJar = target
        }
    }

    val resolvedMojangJar = mojangJar
        ?: error("Could not determine Mojang server jar from Paperclip download context")

    val patchedJar = File(bundleDir, "versions/$minecraftVersion/paper-$minecraftVersion.jar")
    if (!patchedJar.exists()) {
        runPaperclipBuild(paperclipJar, bundleDir)
        require(patchedJar.exists()) {
            "Paperclip did not produce patched jar at ${patchedJar.absolutePath}"
        }
    }

    return PaperclipResources(resolvedMojangJar, patchedJar)
}

private data class DownloadContext(val sha256: String, val url: String, val fileName: String)

private fun readDownloadContexts(paperclipJar: File): List<DownloadContext> {
    return java.util.jar.JarFile(paperclipJar).use { jar ->
        val entry = jar.getJarEntry("META-INF/download-context")
            ?: return emptyList()
        jar.getInputStream(entry).use { input ->
            input.bufferedReader().readLines()
                .mapNotNull { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@mapNotNull null
                    val parts = trimmed.split(Regex("\\s+"))
                    require(parts.size == 3) { "Invalid download-context line: $trimmed" }
                    DownloadContext(parts[0], parts[1], parts[2])
                }
        }
    }
}

private fun ensureFileWithHash(url: String, expectedSha256: String, target: File) {
    if (target.exists() && target.isFile) {
        val actual = sha256(target)
        if (actual.equals(expectedSha256, ignoreCase = true)) {
            return
        }
    }

    target.parentFile?.mkdirs()
    URI.create(url).toURL().openStream().use { input ->
        target.outputStream().use { out ->
            input.copyTo(out)
        }
    }

    val actual = sha256(target)
    require(actual.equals(expectedSha256, ignoreCase = true)) {
        "Checksum mismatch for ${target.absolutePath}. Expected $expectedSha256 but found $actual"
    }
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString(separator = "") { byte ->
        val value = byte.toInt() and 0xFF
        value.toString(16).padStart(2, '0')
    }
}

private fun runPaperclipBuild(paperclipJar: File, workingDir: File) {
    val javaHome = System.getProperty("java.home")
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    val javaExecutable = File(javaHome, "bin/${if (isWindows) "java.exe" else "java"}")
    val process = ProcessBuilder(javaExecutable.absolutePath, "-jar", paperclipJar.absolutePath, "--build")
        .directory(workingDir)
        .inheritIO()
        .start()
    process.waitFor()
}

private fun detectSourceNamespace(mappingFile: File): String {
    mappingFile.bufferedReader().use { reader ->
        val header = reader.readLine()?.trim()
            ?: error("Mapping file ${mappingFile.absolutePath} is empty")
        val parts = header.split('\t')
        require(parts.size >= 5 && parts[0] == "tiny") {
            "Unexpected tiny mapping header in ${mappingFile.absolutePath}: $header"
        }
        return parts[3]
    }
}
