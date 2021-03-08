plugins {
    `java-library`
    `maven-publish`
}

tasks.withType<Wrapper>().configureEach {
    gradleVersion = "6.7"
}

group = "betterfonts"
version = "2.0.0-SNAPSHOT.3"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

repositories {
    if(project.ext.has("podcrashMavenUsername") && project.ext.has("podcrashMavenPassword")) {
        maven {
            setUrl("https://maven.podcrash.com/repository/plus")
            credentials {
                username = project.ext.get("podcrashMavenUsername") as String?
                password = project.ext.get("podcrashMavenPassword") as String?
            }
        }
    }
    mavenCentral()
}

dependencies {

    // Lwjgl

    // Don't need the exact version, I just need it for compiling
    compileOnly("org.lwjgl.lwjgl:lwjgl:2.9.3")

    // Junit

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.5.1")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    repositories {
        if (!project.version.toString().contains("SNAPSHOT") &&
            project.ext.has("podcrashMavenUsername") &&
            project.ext.has("podcrashMavenPassword")
        ) {
            maven {
                setUrl("https://maven.podcrash.com/repository/plus")
                credentials {
                    username = project.ext.get("podcrashMavenUsername") as String?
                    password = project.ext.get("podcrashMavenPassword") as String?
                }
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
