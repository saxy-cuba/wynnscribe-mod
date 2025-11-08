package com.wynnscribe.models

import com.wynnscribe.Translator
import com.wynnscribe.schemas.TranslationRepository
import com.wynnscribe.utils.escapeGroupingRegex
import com.wynnscribe.utils.escapeRegex

interface Placeholder<T> {
    val tag: String

    fun placeholder(text: String): String {
        var num = 1
        return Regex("\\{${tag}(:.+?)?}").replace(text) { "{${tag}:${num++}}" }
    }

    /**
     * パースされたtagをHolderに変換します。
     */
    fun createFragment(tag: ParsedTag, source: TranslationRepository.Translations.Category.Source, categories: List<TranslationRepository.Translations.Category>, struct: Translator.StructMode): Compiled.Fragment<T>

    /**
     * このプレースホルダの正規表現をコンパイル(生成)します。
     */
    fun compile(holders: List<Compiled.Fragment<*>>, source: TranslationRepository.Translations.Category.Source): Compiled<T> {
        val matcher = source.properties.matcher
        if(matcher == null || "{${tag}" !in matcher) { return Compiled(null, emptyList()) }
        val relevantHolders = mutableListOf<Compiled.Fragment<T>>()
        val patternStr = buildString {
            TAG_PATTERN.split(matcher).forEachIndexed { index, str ->
                append(escapeRegex(str))
                val holder = holders.getOrNull(index)
                val pattern = holder?.pattern
                if(holder != null && pattern != null) {
                    if(this@Placeholder != holder.type) {
                        // タグが違う場合は内部のグループをエスケープする
                        append(escapeGroupingRegex(pattern))
                    } else {
                        relevantHolders.add(holder as Compiled.Fragment<T>)
                        append(pattern)
                    }
                }
            }
        }
        return Compiled(patternStr.toRegex(), relevantHolders)
    }

    fun fillPlaceholders(translation: String, sourceText: String, source: TranslationRepository.Translations.Category.Source, categories: List<TranslationRepository.Translations.Category>, struct: Translator.StructMode): String

    class ParsedTag(val key: String, val value: String?) {
        constructor(list: List<String>): this(list[0], list.getOrNull(1))
    }

    companion object {
        val TAG_PATTERN = Regex("(?<!\\\\)\\{((?:\\\\.|[^\\\\{}])*)}")
    }

    class Compiled<T>(val regex: Regex?, val holders: List<Fragment<T>>) {
        class Fragment<T>(val pattern: String, val type: Placeholder<T>, val data: T? = null)
    }
}