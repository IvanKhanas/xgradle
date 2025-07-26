plugins {
    `java-gradle-plugin`
    java
}

group = "org.altlinux.xgradle"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
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

tasks.register<Copy>("copyInitScript"){
    from("src/main/resources/xgradle-plugin.gradle")
    into(layout.buildDirectory.dir("."))
}

tasks.named<Jar>("jar") {
    metaInf {
        from(rootProject.projectDir){
            include("LICENSE")
	    include("NOTICE")
        }
    }

    exclude("xgradle-plugin.gradle")
    archiveFileName.set("xgradle.jar")
}

tasks.named("build"){
    finalizedBy("copyInitScript")
}

tasks.test {
    useJUnitPlatform{
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
