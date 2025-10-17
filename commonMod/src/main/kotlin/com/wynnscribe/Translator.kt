package com.wynnscribe

import com.wynnscribe.CachedItemStackTranslation.Companion.cachedTranslation
import com.wynnscribe.CachedItemStackTranslation.Companion.setCacheTranslation
import com.wynnscribe.schemas.ExportedTranslationSchema
import com.wynnscribe.schemas.Filter
//import com.wynnscribe.Translator.FilterValue.Companion.match
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import kotlin.collections.map

object Translator {

    var Translation: API.TranslationData? = null

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
        val translated =  this.translate(content, filterValue).split("\n").map(MiniMessage::deserialize).map(MinecraftClientAudiences.of()::asNative)
        // ItemStackに翻訳のキャッシュを残す
        itemStack.setCacheTranslation(content, refreshed = Translation!!.at.epochSeconds, translated)
        return translated
    }

    fun filtered(categories: List<ExportedTranslationSchema.Category>, filterValue: FilterValue): List<ExportedTranslationSchema.Category.Source> {
        return categories.filter { it.properties.filter?.match(filterValue.fields)?:true }
            .map { it.sources.filter { it.properties.filter?.match(filterValue.fields)?:true } }
            .flatten()
    }

    fun translate(originalText: String, sources: List<ExportedTranslationSchema.Category.Source>, translationData: API.TranslationData): String {
        var translated = originalText
        for(source in sources) {
            // 翻訳前の文章を残しておく
            val old = translated
            // 一番投票数が多い翻訳を取得
            val translationText = source.best?.text?:source.text
            // 文字列を置換する
            translated = Placeholders.pattern(source, translationData.categories).replace(translated, translationText)
            // replaceが動作したかどうか
            if(old != translated) {
                // プレースホルダ関連を処理
                translated = Placeholders.on(translated, originalText, source, translationData.categories)
                // children関連を処理
                translated = translate(translated, translationData.sourcesByParentId[source.id]?:listOf(), translationData)
            }
            if(source.properties.stopOnMatch && translated != old) {
                break
            }
        }

        return translated
    }

    fun translate(originalText: String, filterValue: FilterValue): String {
        val translationData = Translation?:return originalText
        var translated = originalText
        val sources = filtered(Translation?.categories?:return originalText, filterValue)
        translated = translate(translated, sources, translationData)
        return translated
    }

    /**
     * フィルターにかける値を保管したクラスです。
     */
    data class FilterValue(
        val fields: Map<String, net.kyori.adventure.text.Component>
    )
}