plugins {
    java
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.8"
}

group = "org.altlinux.xgradle"
version = "0.0.1"

repositories {
    mavenCentral()
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly(gradleApi())
    runtimeOnly("org.codehaus.plexus:plexus-utils:3.5.0")
    implementation("org.apache.maven:maven-model-builder:3.8.6")
    implementation("org.apache.maven:maven-model:3.8.6")

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.1")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("xgradle-core/main/java"))
        }
        resources {
            setSrcDirs(listOf("xgradle-core/main/resources"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("xgradle-core/test/java"))
        }
        resources {
            setSrcDirs(listOf("xgradle-core/test/resources"))
        }
    }
}

gradlePlugin{
    isAutomatedPublishing = false
    plugins{
        create("xgradle") {
            id = "org.altlinux.xgradle"
            implementationClass = "org.altlinux.xgradle.plugin.XGradlePlugin"
        }
    }
}

tasks.named<Copy>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from("xgradle-core/main/resources/META-INF/gradle-plugins") {
        include("org.altlinux.xgradle.properties")
    }
}

tasks.register<Copy>("copyInitScript") {
    dependsOn("processResources")
    from("xgradle-core/main/resources/xgradle-plugin.gradle")
    into(layout.buildDirectory.dir("dist"))
}

tasks.shadowJar {
    archiveClassifier.set("")

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
    archiveBaseName.set("xgradle")
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("xgradle") {
            artifact(tasks.shadowJar) {
                builtBy(tasks.shadowJar)
            }
            artifact(tasks.named("javadocJar").get())

            pom {
                name.set("xgradle")
                url.set("https://altlinux.space/ALTLinux/xgradle.git")
                description.set("xgradle plugin for offline build")

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
            groupId = "org.altlinux.xgradle.gradle.plugin"
            artifactId = "org.altlinux.xgradle.gradle.plugin"
            version = project.version.toString()

            pom {
                name.set("XGradle Plugin Marker")
                description.set("Plugin marker for xgradle")
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
                "$artifactId-$version.jar" -> "xgradle.jar"
                "$artifactId-$version.pom" -> "xgradle.pom"
                "$artifactId-$version-javadoc.jar" -> "xgradle-javadoc.jar"
                else -> filename
            }
        }
    }
    into(layout.buildDirectory.dir("dist"))

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    doLast {
        logger.lifecycle("Renamed and copied all publications to ${layout.buildDirectory.dir("dist")}")
    }
}

tasks.named("build") {
    setDependsOn(listOf("assemble"))
    dependsOn("shadowJar", "javadocJar")
    dependsOn("shadowJar", "javadocJar", "copyInitScript", "copyPublicationsToDist")
    finalizedBy("check")
}

tasks.test {
    useJUnitPlatform {
        //Temporary solution wait for 0.0.3
        excludeEngines("junit-vintage")
    }

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
            .file("xgradle-plugin.gradle").get().asFile

        if (targetFile.exists()) {
            targetFile.delete()
        }
    }
}
