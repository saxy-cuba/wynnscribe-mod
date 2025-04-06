package com.wynnscribe

import com.wynnscribe.models.Language
import com.wynnscribe.models.Project
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.time.Duration.Companion.days

object API {
    val httpClient = OkHttpClient()

    const val API_HOST = "http://192.168.0.7:8080" // TODO

    val translationsDir = File("translations")

    fun loadOrDownloadTranslations(language: String): List<Project.Json>? {
        val file = translationsDir.resolve("${language}.json")
        if(file.exists()) {
            val cached = Json.decodeFromString<DownloadedTranslation>(file.readText())
            if(cached.at > Clock.System.now().minus(3.days)) { return cached.translations }
        }
        val downloaded = downloadTranslations(language)
        if(downloaded == null) { return null }
        if(!translationsDir.exists()) { translationsDir.mkdirs() }
        file.writeText(Json.encodeToString(DownloadedTranslation(translations = downloaded)))
        return downloaded
    }

    fun downloadTranslations(language: String): List<Project.Json>? {
        val request = Request.Builder().url("${API_HOST}/api/v1/downloads/${language}.json").get().build()
        val response = httpClient.newCall(request).execute()
        if(response.isSuccessful) {
            val body = response.body?.string()
            if(body != null) {
                return Json.decodeFromString(body)
            }
        }
        return null
    }

    fun languages(): List<Language.Impl>? {
        val request = Request.Builder().url("${API_HOST}/api/v1/languages").get().build()
        val response = httpClient.newCall(request).execute()
        if(response.isSuccessful) {
            val body = response.body?.string()
            if(body != null) {
                return Json.decodeFromString(body)
            }
        }
        return null
    }

    @Serializable
    data class DownloadedTranslation(
        val at: Instant = Clock.System.now(),
        val translations: List<Project.Json>
    )
}