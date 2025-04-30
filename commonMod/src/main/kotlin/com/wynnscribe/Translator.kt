package com.wynnscribe

import com.wynnscribe.CachedItemStackTranslation.Companion.cachedTranslation
import com.wynnscribe.CachedItemStackTranslation.Companion.setCacheTranslation
import com.wynnscribe.Translator.FilterValue.Companion.match
import com.wynnscribe.models.Project
import com.wynnscribe.models.Project.Original
import com.wynnscribe.utils.LegacyTagUtils
import kotlinx.serialization.Serializable
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import kotlin.collections.map

object Translator {

    var Translation: TranslationData? = null

    fun translateItemStackOrCached(itemStack: ItemStack, original: List<Component>, type: Component?, title: Component?): List<Component> {
        if(Translation == null) { return original }
        val content = MiniMessage.serializeList(original.map(MinecraftClientAudiences.of()::asAdventure))
        val cached = itemStack.cachedTranslation(content, refreshed = Translation!!.refreshed)
        if(cached != null) {
            // キャッシュがあった場合はそのItemStackを使う
            return cached
        }
        val filterValue = FilterValue(
            MinecraftClientAudiences.of().asAdventure(type),
            MinecraftClientAudiences.of().asAdventure(title),
            MiniMessage.deserialize(content)
        )
        val translated =  this.translate(content, filterValue).split("\n").map(MiniMessage::deserialize).map(MinecraftClientAudiences.of()::asNative)
        // ItemStackに翻訳のキャッシュを残す
        itemStack.setCacheTranslation(content, refreshed = Translation!!.refreshed, translated)
        return translated
    }

    fun translate(original: List<Component>, type: Component?, title: Component?): List<Component> {
        val content = MiniMessage.serializeList(original.map(MinecraftClientAudiences.of()::asAdventure))

        return translate(content, type, title).split("\n").map(MiniMessage::deserialize).map(MinecraftClientAudiences.of()::asNative)
    }

    fun translate(originalText: String, type: Component?, title: Component?): String {
        val filterValue = FilterValue(
            MinecraftClientAudiences.of().asAdventure(type),
            MinecraftClientAudiences.of().asAdventure(title),
            MiniMessage.deserialize(originalText)
        )
        return this.translate(originalText, filterValue)
    }

    fun filtered(sequence: Sequence<Project.Json>, filterValue: FilterValue): Sequence<Original.Json> {
        return sequence.filter { it.filter.match(filterValue) }.map { it.originals }.flatten().filter { it.filter.match(filterValue) }
    }

    fun translate(originalText: String, originals: Sequence<Original.Json>, translationData: TranslationData): String {
        var translated = originalText
        for(original in originals) {
            // 翻訳前の文章を残しておく
            val old = translated
            // 一番投票数が多い翻訳を取得
            val translationText = original.translations.maxByOrNull { it.score }?.text?.value?:original.template?.value?:continue
            // 文字列を置換する
            translated = Placeholders.pattern(original, translationData.projects).replace(translated, translationText)
            // replaceが動作したかどうか
            if(old != translated) {
                // プレースホルダ関連を処理
                translated = Placeholders.on(translated, originalText, original, translationData.projects)
                // children関連を処理
                translated = translate(translated, original.children.asSequence(), translationData)
            }
            if(original.stopOnMatch && translated != old) {
                break
            }
        }

        return translated
    }

    fun translate(originalText: String, filterValue: FilterValue): String {
        val translationData = Translation?:return originalText
        var translated = originalText
        val originals = filtered(Translation?.projectsSequence?:return originalText, filterValue)
        translated = translate(translated, originals, translationData)
        return translated
    }

    /**
     * フィルターにかける値を保管したクラスです。
     */
    data class FilterValue(
        val type: net.kyori.adventure.text.Component?,
        val title: net.kyori.adventure.text.Component?,
        val content: net.kyori.adventure.text.Component?
    ) {
        fun match(filter: Project.Filter): Boolean {
            if (!this.match(filter.type, this.type)) {
                return false
            }
            if (!this.match(filter.title, this.title)) {
                return false
            }
            if (!this.match(filter.content, this.content)) {
                return false
            }
            return true
        }

        /**
         * Project.Filterとこのクラスの値がマッチしているかどうかをbooleanで返します。
         */
        fun match(filter: Project.Filter.Content?, content: net.kyori.adventure.text.Component?): Boolean {
            if(content == null) { return true }
            if (filter != null) {
                if (filter.fullMatch) {
                    if (filter.withColors) {
                        if(filter.content != MiniMessage.serialize(content)) {
                            return false
                        }
                    } else {
                        if(filter.content != LegacyTagUtils.replaceLegacyTags(PlainTextComponentSerializer.plainText().serialize(content), true)) {
                            return false
                        }
                    }
                } else {
                    if (filter.withColors) {
                        if(filter.content !in MiniMessage.serialize(content)) {
                            return false
                        }
                    } else {
                        if(filter.content !in LegacyTagUtils.replaceLegacyTags(PlainTextComponentSerializer.plainText().serialize(content), true)) {
                            return false
                        }
                    }
                }
            }
            return true
        }

        companion object {
            fun Project.Filter.match(filterValue: FilterValue) = filterValue.match(this)
        }
    }

    /**
     * 保存された翻訳データ
     */
    @Serializable
    data class TranslationData(
        /**
         * 翻訳データの言語。ja_jp形式。
         */
        val lang: String,
        /**
         * データの更新日時。reloadすると更新されます
         */
        val refreshed: Long,
        /**
         * 翻訳一覧
         */
        val projects: List<Project.Json>,
    ) {
        val projectsSequence = projects.asSequence()
    }
}