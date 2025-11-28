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
    id("org.altlinux.xgradle-publishing-conventions")
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
    from("src/main/resources/${rootProject.name}-plugin.gradle")
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

tasks.register<Jar>("sourcesJar") {
    archiveBaseName.set(project.name)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar") {
    archiveBaseName.set(project.name)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

xgradlePublishingConventions {
    projectName.set(project.name)
    projectDescription.set("${rootProject.name} plugin for offline build")
    projectUrl.set("https://altlinux.space/ALTLinux/xgradle.git")

    licenseName.set("The Apache License, Version 2.0")
    licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0")

    enablePluginMarker.set(true)
    enableCopyPublications.set(true)

    withShadowJar()
    withSourcesJar()
    withJavadocJar()

    developer("xeno", "Ivan Khanas", "xeno@altlinux.org")
}

tasks.named("publishToMavenLocal") {
    dependsOn("copyInitScript")
}

tasks.named("build") {
    dependsOn("assemble")
    dependsOn("shadowJar", "javadocJar", "publishToMavenLocal")
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