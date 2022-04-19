repositories.mavenCentral()

plugins {
    id("org.jetbrains.kotlin.jvm")
}

val jvmTarget = "1.8"

tasks.getByName<JavaCompile>("compileJava") {
    targetCompatibility = jvmTarget
}

tasks.getByName<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = jvmTarget
}
