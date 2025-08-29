
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "de.uni_passau.fim.readability"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Add JGit dependency
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    implementation("org.freemarker:freemarker:2.3.31")
    // Other dependencies...
}

configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
    exclude(group = "org.slf4j", module = "slf4j-api")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2024.1.3")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
    }

    signPlugin {

        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    val copyReadabilityModel by registering(Copy::class) {
        from("src/main/resources/readability_model")
        into("build/idea-sandbox/plugins/CodeReadabilityPlugin/bin/readability_model")
    }

    val addTempFolder by registering {
        val tmpDir = File("build/idea-sandbox/plugins/CodeReadabilityPlugin/tmp")
        if (!tmpDir.exists()) {
            tmpDir.mkdirs()
            println("Created temporary folder: ${tmpDir.absolutePath}")
        } else {
            println("Temporary folder already exists: ${tmpDir.absolutePath}")
        }
    }


    build {
        finalizedBy(addTempFolder,copyReadabilityModel)
    }

    runIde {
        dependsOn(copyReadabilityModel)
        dependsOn(addTempFolder)
    }
}
