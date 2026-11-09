plugins {
    java
}

group = "com.apotheotictrims"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/") {
        content { includeGroup("com.github.retrooper") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    compileOnly("com.github.retrooper:packetevents-api:2.13.0")
    testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
    testRuntimeOnly("com.github.retrooper:packetevents-api:2.13.0")
    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.test {
    useJUnitPlatform()
}
