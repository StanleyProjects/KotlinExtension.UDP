buildscript {
    repositories.mavenCentral()

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
    }
}

task<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}
