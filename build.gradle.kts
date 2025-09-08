plugins {
    java
    `maven-publish`
    id ("com.gradleup.shadow") version "8.3.8" apply false
}

allprojects {
    group = "org.altlinux.xgradle"
    version = "0.0.3"

    repositories {
        mavenCentral()
    }
}

subprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        implementation("org.apache.maven:maven-model:3.8.6")
        testImplementation(platform("org.junit:junit-bom:5.10.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    java {
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_11
    }
}

repositories {
    mavenCentral()
}
