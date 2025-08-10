plugins {
    `java-gradle-plugin`
    java
    id("com.gradleup.shadow") version "8.3.8"
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "org.altlinux.xgradle"
version = "0.0.2"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    implementation("org.apache.maven:maven-model-builder:3.8.6")
    implementation("org.apache.maven:maven-model:3.8.6")
    implementation("org.codehaus.plexus:plexus-utils:3.5.0")
    compileOnly(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.1")
}

gradlePlugin{
    plugins{
        create("XGradlePlugin") {
            id = "org.altlinux.xgradle"
            implementationClass = "org.altlinux.gradlePlugin.plugin.XGradlePlugin"
        }
    }
}

tasks.register<Copy>("copyInitScript") {
    from("src/main/resources/xgradle-plugin.gradle")
    into(layout.buildDirectory.dir("."))
}

tasks.shadowJar {
    archiveFileName.set("xgradle.jar")

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

tasks.named("build") {
    dependsOn(tasks.shadowJar)
    finalizedBy("copyInitScript")
}

tasks.test {
    useJUnitPlatform {
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
