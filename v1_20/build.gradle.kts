plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-SNAPSHOT"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    compileOnly("com.mojang:datafixerupper:8.0.16")
    compileOnly("io.netty:netty-transport:4.1.118.Final")

    compileOnly("net.kyori:adventure-api:4.25.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")

    implementation(project(":api"))
    implementation(project(":core"))
}
