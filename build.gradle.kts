import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.apache.tools.ant.filters.ReplaceTokens
import java.io.File
import se.file14.procosmetics.gradle.VersionedObfTask
import se.file14.procosmetics.gradle.ensurePaperDevBundleExtracted
import se.file14.procosmetics.gradle.ensurePaperDevBundleResources

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

    val remapMinecraftVersion = findProperty("remapMinecraftVersion")?.toString()
    if (remapMinecraftVersion != null) {
        extensions.extraProperties["remapMinecraftVersion"] = remapMinecraftVersion

        val mcVersion = remapMinecraftVersion
        val devBundleAttribute = Attribute.of("io.papermc.paperweight.dev-bundle-output", String::class.java)

        val paperDevBundle by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes.attribute(devBundleAttribute, "zip")
        }

        dependencies {
            add(paperDevBundle.name, "io.papermc.paper:dev-bundle:$mcVersion-R0.1-SNAPSHOT")
        }

        val bundleDirProvider = paperDevBundle.elements.map { elements ->
            val bundleFile = elements
                .map { it.asFile }
                .singleOrNull { it.extension == "zip" }
                ?: error("Could not locate Paper dev bundle zip for project $name")
            ensurePaperDevBundleExtracted(project.rootProject.layout.projectDirectory.dir(".gradle/paper-dev-bundles").asFile, mcVersion, bundleFile)
        }

        val paperResourcesProvider = bundleDirProvider.map { bundleDir ->
            ensurePaperDevBundleResources(bundleDir, mcVersion)
        }

        val mojangJarFiles = project.files(paperResourcesProvider.map { it.mojangJar }).builtBy(paperDevBundle)
        val paperServerJarFiles = project.files(paperResourcesProvider.map { it.patchedJar }).builtBy(paperDevBundle)

        dependencies {
            add("compileOnly", mojangJarFiles)
            add("compileOnly", paperServerJarFiles)
            add("compileOnly", "io.papermc.paper:paper-api:$mcVersion-R0.1-SNAPSHOT")
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