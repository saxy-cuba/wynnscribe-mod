package com.wynnscribe

import com.wynnscribe.Placeholder.Companion.TAG_PATTERN
import com.wynnscribe.Placeholder.ParsedTag
import com.wynnscribe.schemas.ExportedTranslationSchema
import com.wynnscribe.utils.escapeGroupingRegex
import com.wynnscribe.utils.escapeRegex
import net.kyori.adventure.text.Component
import net.minecraft.client.Minecraft
import kotlin.sequences.forEach
import kotlin.text.Regex

sealed interface Placeholders {
    companion object {
        // ホントはkotlin-reflex使いたかったけどバンドルサイズがアホだから手動！
        val entries: Map<String, Placeholder<*>> = listOf(Re, In, Player).associateBy { it.tag }

        /**
         * この翻訳をキーとしてreplaceするためのreplace keyを返します。
         */
        fun pattern(source: ExportedTranslationSchema.Category.Source, categories: List<ExportedTranslationSchema.Category>): Regex {
            return returningCompileAll(source, categories, _InternalCompiler).regex!!
        }

        /**
         * プレースホルダーの一覧を取得します。
         */
        fun holders(source: ExportedTranslationSchema.Category.Source, categories: List<ExportedTranslationSchema.Category>): List<Placeholder.Compiled.Holder<*>> {
            val matches = TAG_PATTERN.findAll(source.properties.matcher?:return emptyList())
            val tags = matches.map { ParsedTag(it.groupValues[1].split(":", limit = 2)) }
            val groups = mutableListOf<Placeholder.Compiled.Holder<*>>()
            tags.forEach { tag ->
                groups.add(entries[tag.key]?.holder(tag, source, categories)?:return@forEach)
            }
            return groups
        }

        /**
         * すべてのプレースホルダーをコンパイルします。
         */
        fun compileAll(source: ExportedTranslationSchema.Category.Source, categories: List<ExportedTranslationSchema.Category>) {
            val holders = this.holders(source, categories)
            val regexes = source.regexes ?: return
            regexes.placeholder(_InternalCompiler) { _InternalCompiler.compile(holders, source) }
            entries.forEach { placeholder ->
                regexes.placeholder(placeholder.value, holders, source)
            }
        }

        /**
         * すべてのプレースホルダーをコンパイルします。
         * 更に引数で指定したプレースホルダも返します。
         */
        fun <T> returningCompileAll(source: ExportedTranslationSchema.Category.Source, categories: List<ExportedTranslationSchema.Category>, placeholder: Placeholder<T>): Placeholder.Compiled<T> {
            this.compileAll(source, categories)
            return source.regexes?.placeholder(placeholder) { placeholder.compile(holders(source, categories), source) }?: Placeholder.Compiled(null, emptyList())
        }

        /**
         * プレースホルダを埋める処理に使います。
         */
        fun on(translation: String, sourceText: String, source: ExportedTranslationSchema.Category.Source, categories: List<ExportedTranslationSchema.Category>): String {
            var translated = translation
            entries.values.forEach { placeholder ->
                translated = placeholder.on(translated, sourceText, source, categories)
            }
            return translated
        }
    }

    /**
     * {re:(正規表現)}形式のプレースホルダーを処理する
     */
    object Re: Placeholder<Nothing> {
        override val tag: String = "re"

        override fun holder(
            tag: ParsedTag,
            source: ExportedTranslationSchema.Category.Source,
            categories: List<ExportedTranslationSchema.Category>
        ): Placeholder.Compiled.Holder<Nothing> {
            return Placeholder.Compiled.Holder(tag.value!!, this, null)
        }

        override fun on(
            translation: String,
            sourceText: String,
            source: ExportedTranslationSchema.Category.Source,
            categories: List<ExportedTranslationSchema.Category>
        ): String {
            val regex = source.regexes?.placeholder<Nothing>(this) { returningCompileAll(source, categories, this) }?.regex?:return translation
            var translated = translation
            val matches = regex.findAll(sourceText)
            matches.forEach { match ->
                match.groupValues.forEachIndexed { index, str ->
                    if(index == 0) return@forEachIndexed
                    translated = translated.replaceFirst("{${index}}", str)
                }
            }
            return translated
        }
    }

    object Player: Placeholder<Nothing> {
        override val tag: String = "player"

        override fun holder(
            tag: ParsedTag,
            source: ExportedTranslationSchema.Category.Source,
            categories: List<ExportedTranslationSchema.Category>
        ): Placeholder.Compiled.Holder<Nothing> {
            return Placeholder.Compiled.Holder(Minecraft.getInstance().user.name, this, null)
        }

        override fun on(
            translation: String,
            sourceText: String,
            source: ExportedTranslationSchema.Category.Source,
            categories: List<ExportedTranslationSchema.Category>
        ): String {
            return translation.replaceFirst("{player}", Minecraft.getInstance().user.name)
        }

    }

    /**
     * {in:#wynnscribe.nnn.nnn}形式のプレースホルダーを処理する
     */
    object In: Placeholder<Translator.FilterValue> {
        override val tag: String = "in"

        override fun holder(
            tag: ParsedTag,
            source: ExportedTranslationSchema.Category.Source,
            categories: List<ExportedTranslationSchema.Category>
        ): Placeholder.Compiled.Holder<Translator.FilterValue> {
            val project = tag.value!!
            val filterValue = Translator.FilterValue(mapOf("type" to Component.text(project)))
            // |区切りの値一覧
            val grouping = Translator.filtered(categories, filterValue)
                .mapNotNull {
                    it.regexes?.placeholder(_InternalCompiler) { _InternalCompiler.compile(holders(it, categories), it) }?.regex?.pattern
                }.joinToString("|", transform = ::escapeGroupingRegex)
            return Placeholder.Compiled.Holder("(${grouping})", this, filterValue)
        }

        override fun on(
            translation: String,
            sourceText: String,
            source: ExportedTranslationSchema.Category.Source,
            categories: List<ExportedTranslationSchema.Category>
        ): String {
            val placeholder = source.regexes?.placeholder(this) { returningCompileAll(source, categories, this) }?:return translation
            val holders = placeholder.holders
            val regex = placeholder.regex?:return translation
            var translated = translation
            val matches = regex.findAll(sourceText)
            matches.forEach { match ->
                match.groupValues.forEachIndexed { index, str ->
                    if(index == 0) return@forEachIndexed
                    translated = translated.replaceFirst("{in:${index}}", Translator.translate(str, holders[index-1].data?:return@forEachIndexed))
                }
            }
            return translated
        }
    }

    /**
     * すべてのパターンのグループをエスケープされた状態で出力したいときに使うプレースホルダー
     */
    object _InternalCompiler: Placeholder<Nothing> {
        override val tag: String = "_internal_compiler"

        override fun holder(
            tag: ParsedTag,
            source: ExportedTranslationSchema.Category.Source,
            categories: List<ExportedTranslationSchema.Category>
        ): Placeholder.Compiled.Holder<Nothing> {
            // 使われることはないのでエラー
            throw NotImplementedError()
        }

        override fun on(
            translation: String,
            sourceText: String,
            source: ExportedTranslationSchema.Category.Source,
            categories: List<ExportedTranslationSchema.Category>
        ): String {
            // 使われることはないのでエラー
            throw NotImplementedError()
        }

        /**
         * すべてのグループをエスケープする
         */
        override fun compile(holders: List<Placeholder.Compiled.Holder<*>>, source: ExportedTranslationSchema.Category.Source): Placeholder.Compiled<Nothing> {
            val patternStr = buildString {
                Placeholder.TAG_PATTERN.split(source.properties.matcher?: return Placeholder.Compiled(null, emptyList())).forEachIndexed { index, str ->
                    append(escapeRegex(str))
                    val group = holders.getOrNull(index)
                    val pattern = group?.pattern
                    if(group != null && pattern != null) {
                        append(escapeGroupingRegex(pattern))
                    }
                }
            }
            return Placeholder.Compiled(patternStr.toRegex(), emptyList())
        }
    }


}