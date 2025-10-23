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
plugins {
    `java-gradle-plugin`
}

sourceSets {
    main {
        java {
            srcDirs("main/java")
        }
        resources {
            srcDirs("main/resources")
        }
    }
    test {
        java {
            srcDirs("test/java")
        }
        resources {
            srcDirs("test/resources")
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    implementation(libs.bundles.maven.tooling)
    runtimeOnly(libs.plexus.utils)
    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
}

gradlePlugin{
    isAutomatedPublishing = false
    plugins{
        create(project.name) {
            id = project.group as String
            implementationClass = "${project.group}.plugin.XGradlePlugin"
        }
    }
}

tasks.named<Copy>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from("main/resources/META-INF/gradle-plugins") {
        include("${project.group}.properties")
    }
}

tasks.register<Copy>("copyInitScript") {
    dependsOn("processResources")
    from("main/resources/${rootProject.name}-plugin.gradle")
    into(layout.buildDirectory.dir("dist"))
}

tasks.shadowJar {
    dependsOn("copyInitScript")
    archiveClassifier.set(null as String?)

    metaInf {
        from(rootProject.projectDir) {
            include("LICENSE")
            include("NOTICE")
        }
    }

    minimize()

    exclude(
        "**/*.properties", "**/*.svg",
        "**/*.jpg", "**/*.kotlin_module", "**/*.pro",
        "**/*.template", "**/*.gif", "**/*.bsh",
        "**/*.xml", "**/*.groovy", "**/*.html",
        "**/*.bin", "**/*.json", "**/*.png",
        "**/*.so", "**/*.dll", "groovy*/**",
        "kotlin*/**", "**/*.css", "**/*wrapper.jar",
        "gradle*/**", "**/*.xsl"
    )
    exclude("org/junit/**")
    exclude("org/opentest4j/**")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.register<Jar>("javadocJar") {
    archiveBaseName.set(project.name)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
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
                description.set("${rootProject.name} plugin for offline build")

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

        create<MavenPublication>("pluginMarkerMaven") {
            groupId = "${project.group}.gradle.plugin"
            artifactId = "${project.group}.gradle.plugin"
            version = project.version.toString()

            pom {
                name.set("${rootProject.name} Plugin Marker")
                description.set("Plugin marker for ${rootProject.name}")
                url.set("https://altlinux.space/ALTLinux/xgradle.git")

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
    repositories {
        mavenLocal()
    }
}

tasks.named("publishToMavenLocal") {
    dependsOn("shadowJar", "javadocJar")
}

tasks.register<Copy>("copyPublicationsToDist") {
    dependsOn("shadowJar", "javadocJar", "publishToMavenLocal")

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

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    doLast {
        logger.lifecycle("Renamed and copied all publications to ${layout.buildDirectory.dir("dist").get().asFile.absolutePath}")
    }
}

tasks.named("build") {
    dependsOn("assemble")
    dependsOn("shadowJar", "javadocJar", "copyInitScript", "copyPublicationsToDist")
}

tasks.test {
    mustRunAfter("copyPublicationsToDist")
    useJUnitPlatform ()

    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
    }

    systemProperty("java.library.dir", System.getProperty("java.library.dir"))
    systemProperty("maven.poms.dir", System.getProperty("maven.poms.dir"))
}

tasks.named("clean") {
    doLast {
        val targetFile = layout.buildDirectory
            .file("${rootProject.name}-plugin.gradle").get().asFile

        if (targetFile.exists()) {
            targetFile.delete()
        }
    }
}