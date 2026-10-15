package com.sergioasenjo.vesperhome.music

import kotlinx.serialization.Serializable

@Serializable
data class JellyfinServer(val Address: String, val Id: String, val Name: String, val EndpointAddress: String? = null) {
    val baseUrl: String
        get() {
            val address = Address.trimEnd('/')
            return if (address.startsWith("http://") || address.startsWith("https://")) {
                address
            } else {
                "http://$address:8096"
            }
        }
}

@Serializable
data class JellyfinPublicInfo(val Id: String, val ServerName: String)

@Serializable
data class QuickConnectResult(val Authenticated: Boolean, val Secret: String, val Code: String)

@Serializable
data class QuickConnectRequest(val Secret: String)

@Serializable
data class AuthenticationResult(val User: JellyfinUser, val AccessToken: String, val ServerId: String)

@Serializable
data class JellyfinUser(val Id: String, val Name: String)

data class JellyfinCredentials(val baseUrl: String, val accessToken: String, val userId: String, val serverName: String)

@Serializable
data class JellyfinSeriesResult(val Items: List<JellyfinSeriesItem> = emptyList(), val TotalRecordCount: Int = 0)

@Serializable
data class JellyfinSeriesItem(
    val Id: String,
    val Name: String,
    val ImageTags: Map<String, String> = emptyMap(),
    val PrimaryImageAspectRatio: Double? = null,
    val ProductionYear: Int? = null,
    val EndDate: String? = null,
    val Status: String? = null
)

data class JellyfinSeries(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val imageAspectRatio: Double?,
    val productionYear: Int?,
    val endYear: Int?,
    val continuing: Boolean
)

data class JellyfinShowsState(
    val connected: Boolean = false,
    val series: List<JellyfinSeries> = emptyList(),
    val totalRecordCount: Int = 0,
    val loading: Boolean = false,
    val hasMore: Boolean = false,
    val failed: Boolean = false
)

data class JellyfinSeriesPage(val series: List<JellyfinSeries>, val totalRecordCount: Int)
