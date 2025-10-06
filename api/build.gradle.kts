dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    // Mojang & networking classes used by the API surface
    compileOnly("com.mojang:authlib:6.0.58")
    compileOnly("io.netty:netty-transport:4.1.118.Final")

    compileOnly("net.kyori:adventure-api:4.24.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.24.0")

    // Annotations
    compileOnly("org.jetbrains:annotations:26.0.2")

    //compileOnly("com.github.koca2000:NoteBlockAPI:1.6.3") // temporarily disabled
    compileOnly("com.github.ashtton:NoteBlockAPI:78f2966ccd")
}