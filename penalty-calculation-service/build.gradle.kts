plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

kotlin { jvmToolchain(21) }
springBoot { mainClass.set("ru.inno_sc.penaltycalculation.service.ApplicationKt") }

dependencies {
    implementation(project(":penalty-calculation-api"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.apache.commons:commons-csv:1.14.1")
    implementation("org.apache.poi:poi-ooxml:6.0.0")
    implementation("com.ibm.icu:icu4j:79.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
tasks.bootJar { archiveFileName.set("app.jar") }

tasks.processResources {
    from(rootProject.file("package.json")) { into("static/settings") }
}
