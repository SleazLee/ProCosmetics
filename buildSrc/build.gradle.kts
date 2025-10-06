plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://maven.fabricmc.net/")
}

dependencies {
    implementation("net.fabricmc:tiny-remapper:0.12.0")
}
