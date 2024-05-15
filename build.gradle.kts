import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
}

fun getProperty(propName: String, defaultValue: String? = null): String? {
    return project.findProperty(propName)?.toString() ?: defaultValue
}

korge {
	id = "com.fallout1.demo"
    targetDefault()
	serializationJson()
}


dependencies {
    add("commonMainApi", project(":deps"))
    add("commonMainApi", "com.theokanning.openai-gpt3-java:api:0.18.2")
    add("commonMainApi", "com.theokanning.openai-gpt3-java:service:0.18.2")
    add("commonMainApi", "io.ktor:ktor-client-core:2.0.0")
    add("commonMainApi", "io.ktor:ktor-client-cio:2.0.0")
    add("commonMainApi", "io.ktor:ktor-client-content-negotiation:2.0.0")
    add("commonMainApi", "io.ktor:ktor-serialization-kotlinx-json:2.0.0")
    add("commonMainApi", "com.soywiz.korlibs.korge2:korge")
}

// Export the API_KEY as an environment variable or pass it directly as a build variable
val apiKey = getProperty("API_KEY") ?: error("API_KEY is missing in gradle.properties")
project.extensions.extraProperties["apiKey"] = apiKey

