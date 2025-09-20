plugins {
    application
}

dependencies {
    implementation("org.jcommander:jcommander:2.0")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("com.google.inject:guice:5.1.0")
    runtimeOnly("com.google.guava:guava:33.4.6-jre")
    runtimeOnly("com.google.code.atinject:atinject:1.0.0-rev3")
    runtimeOnly("org.slf4j:log4j-over-slf4j:2.0.16")
    runtimeOnly("aopalliance:aopalliance:1.0")
    runtimeOnly("org.ow2.asm:asm:9.4")
}

application {
    mainClass.set("org.altlinux.xgradle.Main")
}

tasks.shadowJar {
    minimize()
    archiveBaseName.set("xgradle-tool")
    archiveClassifier.set("")
    manifest{
        attributes(
            "Main-Class" to "org.altlinux.xgradle.Main",
            "Implementation-Version" to project.version
        )
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.register<Jar>("javadocJar") {
    archiveBaseName.set("xgradle")
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.register<Task>("createShellScript") {
    dependsOn("shadowJar")
    val outputDir = layout.buildDirectory.dir("dist")
    val scriptFile = outputDir.get().file("xgradle-tool").asFile

    outputs.file(scriptFile)

    doLast {
        val scriptContent = """
            #!/bin/sh
            DIR="$(cd "$(dirname "$0")" && pwd)"
            java -jar "${'$'}DIR/xgradle-tool.jar" "$@"
        """.trimIndent()

        scriptFile.parentFile.mkdirs()
        scriptFile.writeText(scriptContent)
        scriptFile.setExecutable(true)
    }
}

tasks.matching { it.name in listOf("distZip", "distTar", "startScripts") }
    .configureEach {
        dependsOn(tasks.named("shadowJar"))
    }

publishing {
    publications {
        create<MavenPublication>("xgradle-tool") {
            artifactId = "xgradle-tool"

            artifact(tasks.shadowJar) {
                builtBy(tasks.shadowJar)
            }

            pom {
                name.set("xgradle-tool")
                url.set("https://altlinux.space/ALTLinux/xgradle.git")
                description.set("xgradle support tool")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("xeno")
                        name.set("Ivan Khanas")
                        email.set("xeno@altlinux.org")
                    }
                }
            }
        }
    }
}

tasks.register<Copy>("copyPublicationsToDist") {
    dependsOn("shadowJar", "javadocJar", "publishToMavenLocal")
    mustRunAfter("createShellScript")

    val groupPath = project.group.toString().replace('.', '/')
    val artifactId = project.name
    val version = project.version.toString()
    val mavenRepo = File(System.getProperty("user.home"), ".m2/repository")
    val publicationDir = mavenRepo.resolve("$groupPath/$artifactId/$version")

    from(publicationDir) {
        include("$artifactId-$version.jar")
        include("$artifactId-$version.pom")
        include("$artifactId-$version-javadoc.jar")

        rename { filename ->
            when (filename) {
                "$artifactId-$version.jar" -> "xgradle-tool.jar"
                "$artifactId-$version.pom" -> "xgradle-tool.pom"
                "$artifactId-$version-javadoc.jar" -> "xgradle-tool-javadoc.jar"
                else -> filename
            }
        }
    }
    into(layout.buildDirectory.dir("dist"))
}

tasks.named("publishToMavenLocal") {
    dependsOn("shadowJar", "javadocJar")
}

tasks.named("build") {
    dependsOn("copyPublicationsToDist", "createShellScript")
}

tasks.test {
    useJUnitPlatform()
}