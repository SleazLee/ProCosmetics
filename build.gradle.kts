import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSetContainer
import procosmetics.build.VersionedObfTask
import procosmetics.build.ensurePaperDevBundleExtracted
import procosmetics.build.ensurePaperDevBundleResources
import java.io.File

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
    configurations.all {
        resolutionStrategy.capabilitiesResolution.withCapability("org.spigotmc:spigot-api") {
            select("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
            because("Paper API supersedes Spigot API")
        }
    }
    val javaVersion = findProperty("javaVersion")?.toString()?.toIntOrNull() ?: 21

    tasks.withType<JavaCompile>().configureEach {
        javaCompiler.set(javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        })
    }

    val remapVersion = findProperty("remapMinecraftVersion")?.toString()
    if (remapVersion != null) {
        extensions.extraProperties["remapMinecraftVersion"] = remapVersion
        val paperDevBundle by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false
        }

        dependencies {
            add(paperDevBundle.name, "io.papermc.paper:dev-bundle:$remapVersion-R0.1-SNAPSHOT@zip")
        }

        val bundleCacheRoot = project.rootProject.layout.projectDirectory.dir(".gradle/paper-dev-bundles").asFile
        val bundleFile = paperDevBundle.singleFile
        val extractedDir = ensurePaperDevBundleExtracted(bundleCacheRoot, remapVersion, bundleFile)
        val paperclipResources = ensurePaperDevBundleResources(extractedDir, remapVersion)
        val spigotFiles = project.files(paperclipResources.patchedJar)

        dependencies {
            add("compileOnly", spigotFiles)
        }

        tasks.withType<JavaCompile>().configureEach {
            classpath = classpath.plus(spigotFiles)
        }

        extensions.extraProperties["paperclipResources"] = paperclipResources

        project.extensions.getByType(SourceSetContainer::class.java).named("main") {
            compileClasspath += spigotFiles
        }

        extensions.extraProperties["paperDevBundleConfig"] = paperDevBundle
    }

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