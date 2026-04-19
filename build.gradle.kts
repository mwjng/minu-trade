plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.minupay"
version = "0.0.1-SNAPSHOT"
description = "Minu Trade Stock Trading Service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["resilience4jVersion"] = "2.2.0"

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // JPA / MySQL
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")

    // MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // Elasticsearch
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Resilience4j (Circuit Breaker, Retry, TimeLimiter)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-reactor:${property("resilience4jVersion")}")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // QueryDSL (Jakarta)
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:mongodb")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:elasticsearch")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
