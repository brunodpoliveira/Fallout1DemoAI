import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
}

korge {
    entryPoint = "main"
    jvmMainClassName = "MainKt"
    id = "com.fallout1.demo"
    //targetAll()
    //targetDefault()
    targetJvm()
    //targetJs()
    targetWasm()
    targetDesktop()
    //targetIos()
    //targetAndroid()

	serializationJson()
}

dependencies {
    add("commonMainApi", project(":deps"))
    add("commonMainApi", "io.github.lambdua:service:0.22.3")
    add("commonMainApi", "io.github.lambdua:client:0.22.3")
    add("commonMainApi", "io.github.lambdua:api:0.22.3")
    //add("commonMainApi", "io.ktor:ktor-client-core:2.0.0")
    //add("commonMainApi", "io.ktor:ktor-client-cio:2.0.0")
    //add("commonMainApi", "io.ktor:ktor-client-content-negotiation:2.0.0")
    //add("commonMainApi", "io.ktor:ktor-serialization-kotlinx-json:2.0.0")
    //add("commonMainApi", "com.soywiz.korlibs.korge2:korge")
}
