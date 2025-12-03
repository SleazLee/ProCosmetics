plugins {
    id("io.papermc.paperweight.userdev") version "1.7.2"
}

configurations.named("paperweightDevelopmentBundle") {
    attributes.attribute(org.gradle.api.attributes.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, org.gradle.api.attributes.ArtifactTypeDefinition.ZIP_TYPE)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")

    compileOnly("net.kyori:adventure-api:4.25.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")

    implementation(project(":api"))
    implementation(project(":core"))
}
