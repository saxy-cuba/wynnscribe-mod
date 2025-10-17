package com.wynnscribe

import com.wynnscribe.schemas.ExportedTranslationSchema
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

    const val API_HOST = "http://100.67.211.101:8787" // TODO

    val translationsDir = File("translations")

    fun loadOrDownloadTranslations(language: String): TranslationData? {
        val file = translationsDir.resolve("${language}.json")
        if(file.exists()) {
            val cached = Json.decodeFromString<TranslationData>(file.readText())
            if(cached.at > Clock.System.now().minus(3.days)) { return cached }
        }
        val downloaded = downloadTranslations(language) ?: return null
        if(!translationsDir.exists()) { translationsDir.mkdirs() }
        val translationData = TranslationData(data = downloaded)
        file.writeText(Json.encodeToString(translationData))
        return translationData
    }

    fun downloadTranslations(language: String): ExportedTranslationSchema? {
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

    @Serializable
    data class TranslationData(
        val data: ExportedTranslationSchema,
        val at: Instant = Clock.System.now()
    ) {
        val sourcesMap: MutableMap<String, ExportedTranslationSchema.Category.Source> = mutableMapOf()

        val sources = mutableListOf<ExportedTranslationSchema.Category.Source>()

        val categoriesMap: MutableMap<String, ExportedTranslationSchema.Category> = mutableMapOf()

        val categories = mutableListOf<ExportedTranslationSchema.Category>()

        val sourcesByParentId = mutableMapOf<String, MutableList<ExportedTranslationSchema.Category.Source>>()

        init {
            this.data.categories.forEach { category ->
                this.categoriesMap[category.id] = category
                this.categories.add(category)
                category.sources.forEach { source ->
                    this.sourcesMap[source.id] = source
                    this.sources.add(source)
                    if(source.parentId != null) {
                        sourcesByParentId.getOrPut(source.parentId) { mutableListOf() }.add(source)
                    }
                }
            }
        }
    }
}