import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.4.4"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	id("idea")
	kotlin("jvm") version "1.4.31"
	kotlin("plugin.spring") version "1.4.31"
	kotlin("plugin.serialization") version "1.4.31"
}

group = "ehn.techiop.hcert"
version = "0.1.7-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

idea {
	module {
		isDownloadSources = true
		isDownloadJavadoc = true
	}
}

repositories {
    mavenLocal()
    jcenter()
	mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/ehn-digital-green-development/hcert-kotlin")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.1.0")
    implementation("com.google.zxing:core:3.4.1")
	implementation("ehn.techiop.hcert:hcert-kotlin:0.1.7-SNAPSHOT")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("com.augustcellars.cose:cose-java:1.1.0")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict", "-Xopt-in=kotlin.ExperimentalUnsignedTypes", "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi")
		jvmTarget = "1.8"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
