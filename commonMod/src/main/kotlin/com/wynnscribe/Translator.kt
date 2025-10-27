package com.wynnscribe

import com.wynnscribe.CachedItemStackTranslation.Companion.cachedTranslation
import com.wynnscribe.CachedItemStackTranslation.Companion.setCacheTranslation
import com.wynnscribe.schemas.ExportedTranslationSchema
import com.wynnscribe.schemas.Filter
import com.wynntils.core.components.Models
import com.wynntils.core.text.StyledText
import com.wynntils.core.text.StyledText.fromComponent
import kotlinx.serialization.json.Json
//import com.wynnscribe.Translator.FilterValue.Companion.match
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import kotlin.collections.map

object Translator {

    var Translation: API.TranslationData? = null

    val PlainTextSerializer = PlainTextComponentSerializer.plainText()

    private fun sha256(input: String): String {
        val hexChars = "0123456789ABCDEF"
        val bytes = MessageDigest
            .getInstance("sha256")
            .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }

    val caches = mutableMapOf<String, CompletableFuture<List<StyledText>>>()

    var history: List<String> = listOf()

    private val TEXT_DISPLAY_TYPE_MAP = FilterValue(mapOf("type" to net.kyori.adventure.text.Component.text("#wynnscribe.textdisplay")))
    private val CHAT_TYPE_MAP = FilterValue(mapOf("type" to net.kyori.adventure.text.Component.text("#wynnscribe.chat")))

    fun translateNpcDialogue(styledTexts: List<StyledText>): CompletableFuture<List<StyledText>> {
        val completableFuture = CompletableFuture<List<StyledText>>()

        val dialogue = styledTexts.map(StyledText::getComponent).map(MinecraftClientAudiences.of()::asAdventure)
        var (plain,plainPairs) = Processing.preprocessing(dialogue.joinToString("\n", transform = PlainTextSerializer::serialize))
        val (message, messagePairs) = Processing.preprocessing(MiniMessage.serializeList(dialogue))

        val dialogRegex = Regex("(?:\\{progress}|)(?: |)(.+?): (.+)")
        val groupValues = dialogRegex.find(plain)?.groupValues
        var speaker: String? = null
        var quest: String? = null
        val target = Minecraft.getInstance().languageManager.selected
        val progress = messagePairs["{progress}"]

        val cacheId = "${target}:${"dialog"}:${speaker?:"none"}:${progress?:"none"}:${sha256(message)}"

        if(groupValues != null) {
            speaker = groupValues[1]
            plain = groupValues[2]
        }
        if(Models.Activity.isTracking) {
            quest = Models.Activity.trackedName
        }

        val cached = caches[cacheId]

        if(cached != null) {
            return cached
        } else {
            caches[cacheId] = completableFuture
        }

        val light = message.endsWith(plain)

        CompletableFuture.runAsync {
            try {
                val requestBody = API.AITranslation.Request(
                    text = if (light) plain else message,
                    plain = plain,
                    speaker = speaker,
                    quest = quest,
                    type = API.AITranslation.Type.DIALOG,
                    history = this.history,
                    target = target,
                    progress = progress
                )

                var historyElement: String

                if (light) {
                    historyElement = speaker?.let { "${it}: $plain" } ?: plain
                    this.history = history.take(19) + historyElement
                } else {
                    historyElement = plain
                    this.history = history.take(19) + historyElement
                }

                val response = API.aiTranslation(requestBody)

                if (response != null) {
                    var translated = response.translated
                    if (light) {
                        translated = message.replace(plain, translated)
                    }

                    val translatedComponent = Processing.postprocessing(translated, messagePairs).split("\n").map(MiniMessage::deserialize)

                    val result = translatedComponent.map(MinecraftClientAudiences.of()::asNative)

                    this.history = this.history.dropLast(1)
                    this.history = this.history.takeLast(19) + translatedComponent.joinToString("<br/>", transform = PlainTextSerializer::serialize)

                    completableFuture.complete(result.map(StyledText::fromComponent))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                completableFuture.complete(styledTexts)
            }
        }

        return completableFuture
    }

    fun translateActivity(components: MutableList<Component>) {
        val activity = components.map(MinecraftClientAudiences.of()::asAdventure)
        var (plain,plainPairs) = Processing.preprocessing(activity.joinToString("\n", transform = PlainTextSerializer::serialize))
        var (message, messagePairs) = Processing.preprocessing(MiniMessage.serializeList(activity))
        var quest: String? = null
        val light = message.endsWith(plain)
        val target = Minecraft.getInstance().languageManager.selected
        if(Models.Activity.isTracking) {
            quest = Models.Activity.trackedName
        }

        val requestBody = API.AITranslation.Request(
            text = if (light) plain else message,
            plain = plain,
            speaker = "Activity",
            quest = quest,
            type = API.AITranslation.Type.DIALOG,
            history = emptyList(),
            target = target,
            progress = null
        )

        val response = API.aiTranslation(requestBody)

        if(response != null) {
            var translated = response.translated
            if (light) {
                translated = message.replace(plain, translated)
            }
            val translatedComponent = Processing.postprocessing(translated, messagePairs).split("\n").map(MiniMessage::deserialize)

            val result = translatedComponent.map(MinecraftClientAudiences.of()::asNative)
            components.clear()
            components.addAll(result)
        }
    }

    fun translateTextDisplay(component: Component): Component {
        if(Translation == null) { return component }
        val text = MiniMessage.serialize(MinecraftClientAudiences.of().asAdventure(component))
        if(text.isEmpty()) return component
//        println("テキストディスプレイ！！！")
//        println(text)
        val translated = this.translate(text, TEXT_DISPLAY_TYPE_MAP, struct = StructMode.StructCategory)
        return MinecraftClientAudiences.of().asNative(MiniMessage.deserialize(translated))
    }

    fun translateChat(component: Component): Component {
        if(Translation == null) { return component }
        val (text, tags) = Processing.preprocessing(MiniMessage.serialize(MinecraftClientAudiences.of().asAdventure(component)), resetPerLines = false, newLineCode = "\n")
        if(text.isEmpty()) return component
        println("チャット！！！ ==== ")
        println(text)
        val translated = this.translate(text, CHAT_TYPE_MAP, struct = StructMode.StructCategory)
        return MinecraftClientAudiences.of().asNative(MiniMessage.deserialize(Processing.postprocessing(translated, tags)))
    }

    fun translateItemStackOrCached(itemStack: ItemStack, original: List<Component>, type: String?): List<Component> {
        if(Translation == null) { return original }
        val content = MiniMessage.serializeList(original.map(MinecraftClientAudiences.of()::asAdventure))
        val cached = itemStack.cachedTranslation(content, refreshed = Translation!!.at.epochSeconds)
        if(cached != null) {
            // キャッシュがあった場合はそのItemStackを使う
            return cached
        }
        val fields = mutableMapOf<String, net.kyori.adventure.text.Component>()
        val inventoryName = Minecraft.getInstance().screen?.title
        if(inventoryName != null) {
            fields["inventoryName"] = MinecraftClientAudiences.of().asAdventure(inventoryName)
        }
        val displayName = original.firstOrNull()
        if(displayName != null) {
            fields["displayName"] = MinecraftClientAudiences.of().asAdventure(displayName)
        }
        if(type != null) {
            fields["type"] = net.kyori.adventure.text.Component.text(type)
        }

        val filterValue = FilterValue(fields)
        val translated =  this.translate(content, filterValue, struct = StructMode.None).split("\n").map(MiniMessage::deserialize).map(MinecraftClientAudiences.of()::asNative)
        // ItemStackに翻訳のキャッシュを残す
        itemStack.setCacheTranslation(content, refreshed = Translation!!.at.epochSeconds, translated)
        return translated
    }

    /**
     * @struct trueの場合filterが厳格になります。
     */
    fun filtered(categories: List<ExportedTranslationSchema.Category>, filterValue: FilterValue, struct: StructMode): List<ExportedTranslationSchema.Category.Source> {
        return categories
            .filter { it.properties.filter?.match(filterValue.fields)?:struct.category }
            .map { it.sourcesWithoutChild.filter { it.properties.filter?.match(filterValue.fields)?:struct.source } }
            .flatten().sortedByDescending { it.properties.priority }
    }

    fun translate(originalText: String, sources: List<ExportedTranslationSchema.Category.Source>, translationData: API.TranslationData, struct: StructMode): String {
        var translated = originalText
        for(source in sources) {
            // 翻訳前の文章を残しておく
            val old = translated
            // 一番投票数が多い翻訳を取得
            val translationText = source.best?.text?:source.text
            // 文字列を置換する
            translated = Placeholders.pattern(source, translationData.categories, struct = struct)?.replace(translated, translationText)?:translated.replace(source.text, translationText)
            // replaceが動作したかどうか
            if(old != translated) {
                // プレースホルダ関連を処理
                translated = Placeholders.on(translated, originalText, source, translationData.categories, struct = struct)
                // children関連を処理
                translated = translate(translated, translationData.sourcesByParentId[source.id]?:listOf(), translationData, struct = struct)
            }
            if(source.properties.stopOnMatch && translated != old) {
                break
            }
        }

        return translated
    }

    fun translate(originalText: String, filterValue: FilterValue, struct: StructMode): String {
        val translationData = Translation?:return originalText
        var translated = originalText
        val sources = filtered(Translation?.categories?:return originalText, filterValue, struct = struct)
        translated = translate(translated, sources, translationData, struct = struct)
        return translated
    }

    /**
     * フィルターにかける値を保管したクラスです。
     */
    data class FilterValue(
        val fields: Map<String, net.kyori.adventure.text.Component>
    )

    enum class StructMode(val category: Boolean, val source: Boolean) {
        None(true, true),
        StructCategory(false, true),
        StructSource(true, false),
        StructBoth(false, false)
    }
}