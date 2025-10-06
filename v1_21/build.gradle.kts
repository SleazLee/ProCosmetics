dependencies {
    compileOnly("net.kyori:adventure-api:4.24.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")
    compileOnly("com.mojang:authlib:6.0.58")
    compileOnly("com.mojang:datafixerupper:6.0.6")
    compileOnly("io.netty:netty-transport:4.1.118.Final")

    implementation(project(":api"))
    implementation(project(":core"))
}
