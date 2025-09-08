dependencies {
    implementation("org.jcommander:jcommander:2.0")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.3")
    implementation("com.google.inject:guice:5.1.0")
}

tasks.test {
    useJUnitPlatform()
}