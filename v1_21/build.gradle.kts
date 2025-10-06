dependencies {
    compileOnly("net.kyori:adventure-api:4.24.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")

    implementation(project(":api"))
    implementation(project(":core"))
}
