import org.apache.groovy.lang.annotation.Incubating

/*
 * Copyright 2025 BaseALT Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    application
}

dependencies {
    implementation(libs.bundles.maven.tooling)
    implementation(libs.bundles.logging)
    implementation(libs.jcommander)
    implementation(libs.plexus.utils)
    implementation(libs.guice)
    runtimeOnly(libs.bundles.guice.deps)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
}

val gitCommitId = createCommitIdProvider()
val buildTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(Date())

fun createCommitIdProvider(): Provider<String> {
    return providers.systemProperty("gitCommitId")
        .orElse(providers.provider {
        runGitCommand("rev-parse", "HEAD~0")
    })
        .orElse("<Unknown>")
        .map {it.trim()}
        .filter { it.isNotBlank() }
}

private fun runGitCommand(vararg args: String): String? {
    return try {
        val process = ProcessBuilder("git", *args)
            .directory(rootProject.projectDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()

        process.waitFor(10, TimeUnit.SECONDS)
        if (process.exitValue() == 0) {
            process.inputStream.bufferedReader().use {
                it.readText().trim()
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

tasks.processResources {
    filesMatching("application.properties") {
        expand(
            "projectVersion" to project.version,
            "gitCommitHash" to gitCommitId.get(),
            "projectBuildTime" to buildTime
        )
    }
}

application {
    mainClass.set("${project.group}.Main")
}

tasks.shadowJar {
    dependsOn(tasks.processResources)

    minimize()

    archiveBaseName.set(project.name)
    archiveClassifier.set(null as String?)

    manifest{
        attributes(
            "Main-Class" to "${project.group}.Main",
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
    val scriptFile = outputDir.get().file(project.name).asFile

    outputs.file(scriptFile)

    doLast {
        val scriptContent = """
            #!/bin/sh
            target="${'$'}0"
            while [ -L "${'$'}target" ]; do
                link=$(readlink "${'$'}target") || exit 1
                case "${'$'}link" in
                    /*) target="${'$'}link" ;;
                    *)  target=$(dirname "${'$'}target")/"${'$'}link" ;;
                esac
            done
            DIR=$(cd "$(dirname "${'$'}target")" && pwd)
            exec java -jar "${'$'}DIR/${project.name}.jar" "${'$'}@"
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
        create<MavenPublication>(project.name) {
            artifactId = project.name

            artifact(tasks.shadowJar) {
                builtBy(tasks.shadowJar)
            }

            artifact(tasks.named("javadocJar").get())

            pom {
                name.set(project.name)
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
    dependsOn("shadowJar", "javadocJar", "publishToMavenLocal", "createShellScript")

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
                "$artifactId-$version.jar" -> "$artifactId.jar"
                "$artifactId-$version.pom" -> "$artifactId.pom"
                "$artifactId-$version-javadoc.jar" -> "$artifactId-javadoc.jar"
                else -> filename
            }
        }
    }
    into(layout.buildDirectory.dir("dist"))

    doLast {
        logger.lifecycle("Renamed and copied all publications to ${layout.buildDirectory.dir("dist").get().asFile.absolutePath}")
    }
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
