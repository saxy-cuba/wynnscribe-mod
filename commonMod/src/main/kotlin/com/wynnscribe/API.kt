package com.wynnscribe

import com.wynnscribe.schemas.ExportedTranslationSchema
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

object API {
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(30))
        .writeTimeout(Duration.ofSeconds(30))
        .callTimeout(Duration.ofSeconds(30))
        .build()

    val translationsDir = File("translations")

    val json = Json { ignoreUnknownKeys = true }

    private var accountToken: String? = null
    private var expiresAt: Instant = Clock.System.now()

    fun loadOrDownloadTranslations(language: String): TranslationData? {
        val file = translationsDir.resolve("${language}.json")
        if(file.exists()) {
            val cached = json.decodeFromString<TranslationData>(file.readText())
            if(cached.at > Clock.System.now().minus(3.days)) { return cached }
        }
        val downloaded = downloadTranslations(language) ?: return null
        if(!translationsDir.exists()) { translationsDir.mkdirs() }
        val translationData = TranslationData(data = downloaded)
        file.writeText(json.encodeToString(translationData))
        return translationData
    }

    val JsonType = "application/json".toMediaType()

    fun generateAccountToken(accessToken: String) {
        val request = Request.Builder().url("https://api.wynnscribe.com/api/v1/verify/minecraft")
            .header("X-Wynnscribe-Requester", "Mod")
            .post(json.encodeToString(AccountToken.Request(accessToken = accessToken)).toRequestBody(JsonType)).build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string()
        if(body != null) {
            this.accountToken = json.decodeFromString<AccountToken.Response>(body).token
            this.expiresAt = Clock.System.now().plus(23.hours)
        }
    }

    fun getAccountToken(): String? {
        if(this.accountToken == null || this.expiresAt < Clock.System.now()) {
            this.generateAccountToken(Minecraft.getInstance().user?.accessToken?:return null)
            return this.accountToken
        }
        return this.accountToken
    }

    fun aiTranslation(request: AITranslation.Request):AITranslation.Response? {
        val accountToken = this.getAccountToken()?:return null
        println("====")
        println(json.encodeToString(request))
        println("====")
        val start = System.currentTimeMillis()
        val request = Request.Builder().url("https://api.wynnscribe.com/api/v1/experiments/ai/translation")
            .header("X-Wynnscribe-Requester", "Mod")
            .header("x-minecraft-authorization", "Bearer $accountToken")
            .post(json.encodeToString(request).toRequestBody(JsonType)).build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string()
        if(body != null) {
            println("${System.currentTimeMillis() - start}msで終わりました！！！")
            println(body)
            val response = json.decodeFromString<AITranslation.Response>(body)
            return response
        }
        return null
    }

    fun downloadTranslations(language: String): ExportedTranslationSchema? {
        val request = Request.Builder().url("https://storage.wynnscribe.com/${language}.json").header("x-wynnscribe-requester", "Mod").get().build()
        val response = httpClient.newCall(request).execute()
        if(response.isSuccessful) {
            val body = response.body?.string()
            if(body != null) {
                return json.decodeFromString(body)
            }
        }
        return null
    }

    class AccountToken() {
        @Serializable
        data class Request(
            @SerialName("accessToken")
            val accessToken: String
        )

        @Serializable
        data class Response(
            val token: String
        )
    }

    class AITranslation() {
        @Serializable
        data class Request(
            val text: String,
            val plain: String,
            val speaker: String?,
            val quest: String?,
            val type: Type,
            val history: List<String>,
            val target: String,
            val progress: String?
        )

        @Serializable
        enum class Type {
            @SerialName("dialog")
            DIALOG,
            @SerialName("lore")
            LORE
        }

        @Serializable
        data class Response(
            val translated: String
        )
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
                category.sources.sortedByDescending { it.properties.priority }.forEach { source ->
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

fun main() {
    val json = Json { ignoreUnknownKeys = true }
    json.decodeFromString<API.TranslationData>(File("C:\\Users\\cub4\\IdeaProjects\\wynnscribe-mod\\fabric\\run\\translations\\ja_jp.json").readText())
}