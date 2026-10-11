package com.lumenui.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Backend(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val protocol: String = "http",
    val host: String = "",
    val port: Int = 9090,
    val secret: String = "",
    val secondaryPath: String = ""
) {
    val baseUrl: String
        get() {
            val path = secondaryPath.trim().let {
                when {
                    it.isEmpty() -> ""
                    it.startsWith("/") -> it
                    else -> "/$it"
                }
            }
            return "$protocol://$host:$port$path"
        }

    val displayName: String
        get() = name.ifBlank { "$host:$port" }
}
