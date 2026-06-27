plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
  runServer {
    minecraftVersion("26.1.2")
  }
}