import dev.detekt.gradle.Detekt

plugins {
    kotlin("jvm") version "2.4.0" apply false
    kotlin("plugin.spring") version "2.4.0" apply false
    id("org.springframework.boot") version "4.0.9" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("dev.detekt") version "2.0.0-alpha.5" apply false
}

subprojects {
    apply(plugin = "dev.detekt")

    dependencies {
        "detektPlugins"("dev.detekt:detekt-rules-ktlint-wrapper:2.0.0-alpha.5")
    }

    tasks.withType<Detekt>().configureEach {
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(true)
        parallel.set(true)
        autoCorrect.set(true)
    }
}

allprojects {
    group = "ru.inno_sc"
    version = "0.1.9"
    repositories { mavenCentral() }
}
