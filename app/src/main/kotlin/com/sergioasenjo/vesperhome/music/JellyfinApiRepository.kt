package com.sergioasenjo.vesperhome.music

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class JellyfinApiRepository(
    private val client: OkHttpClient,
    private val json: Json,
    private val preferencesRepository: JellyfinPreferencesRepository
) {
    suspend fun resolveServer(address: String): JellyfinServer {
        val normalizedAddress = address.trim().trimEnd('/').let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }
        val info: JellyfinPublicInfo = execute(normalizedAddress, "System/Info/Public")
        return JellyfinServer(Address = normalizedAddress, Id = info.Id, Name = info.ServerName)
    }

    suspend fun initiateQuickConnect(server: JellyfinServer): QuickConnectResult = execute(
        server.baseUrl,
        "QuickConnect/Initiate",
        method = HttpMethod.POST
    )

    suspend fun checkQuickConnect(server: JellyfinServer, secret: String): QuickConnectResult = execute(
        server.baseUrl,
        "QuickConnect/Connect",
        query = mapOf("secret" to secret)
    )

    suspend fun authenticate(server: JellyfinServer, secret: String): AuthenticationResult = execute(
        server.baseUrl,
        "Users/AuthenticateWithQuickConnect",
        method = HttpMethod.POST,
        body = json.encodeToString(QuickConnectRequest(secret))
    )

    suspend fun series(credentials: JellyfinCredentials, startIndex: Int, limit: Int): JellyfinSeriesPage {
        val result: JellyfinSeriesResult = execute(
            credentials.baseUrl,
            "Items",
            token = credentials.accessToken,
            query = mapOf(
                "userId" to credentials.userId,
                "includeItemTypes" to "Series",
                "recursive" to "true",
                "sortBy" to "SortName",
                "sortOrder" to "Ascending",
                "startIndex" to startIndex.toString(),
                "limit" to limit.toString(),
                "fields" to "PrimaryImageAspectRatio,ProductionYear,EndDate,Status",
                "enableImages" to "true",
                "imageTypeLimit" to "1",
                "enableImageTypes" to "Primary"
            )
        )
        val series = result.Items.map { item ->
            val imageUrl = item.ImageTags[PRIMARY_IMAGE_TYPE]?.let { tag ->
                seriesImageUrl(credentials, item.Id, PRIMARY_IMAGE_TYPE, tag)
            }
            val endYear = item.EndDate?.substringBefore('-')?.toIntOrNull()
            JellyfinSeries(
                id = item.Id,
                name = item.Name,
                imageUrl = imageUrl,
                imageAspectRatio = item.PrimaryImageAspectRatio,
                productionYear = item.ProductionYear,
                endYear = endYear,
                continuing = item.Status == CONTINUING_STATUS
            )
        }
        return JellyfinSeriesPage(series, result.TotalRecordCount)
    }

    private fun seriesImageUrl(
        credentials: JellyfinCredentials,
        seriesId: String,
        imageType: String,
        tag: String
    ): String = credentials.baseUrl.toHttpUrl().newBuilder()
        .addPathSegments("Items/$seriesId/Images/$imageType")
        .addQueryParameter("tag", tag)
        .addQueryParameter("maxWidth", SERIES_IMAGE_WIDTH.toString())
        .addQueryParameter("quality", IMAGE_QUALITY.toString())
        .addQueryParameter("api_key", credentials.accessToken)
        .build()
        .toString()

    private suspend inline fun <reified T> execute(
        baseUrl: String,
        path: String,
        method: HttpMethod = HttpMethod.GET,
        token: String? = null,
        query: Map<String, String> = emptyMap(),
        body: String? = null
    ): T = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl().newBuilder().addPathSegments(path).apply {
            query.forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
        val requestBody = body.orEmpty().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", authorizationHeader(token))
            .apply {
                if (method == HttpMethod.POST) post(requestBody)
            }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Jellyfin request failed with ${response.code}")
            json.decodeFromString<T>(response.body.string())
        }
    }

    private suspend fun authorizationHeader(token: String?): String {
        val deviceId = preferencesRepository.deviceId()
        return buildString {
            append("MediaBrowser Client=\"Vesper Home\", Device=\"Android TV\", ")
            append("DeviceId=\"").append(deviceId).append("\", Version=\"0.1.0\"")
            if (token != null) append(", Token=\"").append(token).append('"')
        }
    }

    private enum class HttpMethod {
        GET,
        POST
    }

    private companion object {
        const val SERIES_IMAGE_WIDTH = 480
        const val PRIMARY_IMAGE_TYPE = "Primary"
        const val CONTINUING_STATUS = "Continuing"
        const val IMAGE_QUALITY = 90
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
