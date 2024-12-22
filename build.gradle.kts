import korlibs.korge.gradle.*

plugins {
    alias(libs.plugins.korge)
}

korge {
    entryPoint = "main"
    jvmMainClassName = "MainKt"
    id = "com.fallout1.demo"
    targetJvm()
    targetWasm()
    targetDesktop()
    serializationJson()
}

val openApiKey: String = project.findProperty("open.api.key") as String

tasks.withType<JavaExec> {
    environment("openApiKey", openApiKey)
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from("./") {
        include("config.properties")
    }
    from("./setup") {
        include("*.bat")
        include("*.sh")
        into("setup")
    }
    from("src/commonMain/resources/setup") {
        include("*.bat")
        include("*.sh")
        into("setup")
    }
    doLast {
        fileTree("build/libs").matching {
            include("**/*.sh")
        }.forEach { it.setExecutable(true) }
    }
}

dependencies {
    add("commonMainApi", project(":deps"))
    add("commonMainApi", "io.github.lambdua:service:0.22.3")
    add("commonMainApi", "io.github.lambdua:client:0.22.3")
    add("commonMainApi", "io.github.lambdua:api:0.22.3")
    add("commonMainApi", "com.squareup.okhttp3:okhttp:4.9.3")
    add("commonMainApi", "org.json:json:20240303")
}
