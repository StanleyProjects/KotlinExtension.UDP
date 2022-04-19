repositories.mavenCentral()

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass.set("sp.service.sample.AppKt")
}

val jvmTarget = "1.8"

tasks.getByName<JavaCompile>("compileJava") {
    targetCompatibility = jvmTarget
}

tasks.getByName<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = jvmTarget
}
