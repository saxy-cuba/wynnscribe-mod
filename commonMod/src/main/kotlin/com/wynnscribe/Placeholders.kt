package com.wynnscribe

import com.wynnscribe.Placeholder.Companion.TAG_PATTERN
import com.wynnscribe.Placeholder.ParsedTag
import com.wynnscribe.models.Project
import com.wynnscribe.models.Project.Original.Json.Regexes.CompiledPlaceholder
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
        fun pattern(original: Project.Original.Json, projects: List<Project.Json>): Regex {
            return returningCompileAll(original, projects, _InternalCompiler).regex!!
        }

        /**
         * プレースホルダーの一覧を取得します。
         */
        fun holders(original: Project.Original.Json, projects: List<Project.Json>): List<CompiledPlaceholder.Holder<*>> {
            val matches = TAG_PATTERN.findAll(original.text.value)
            val tags = matches.map { ParsedTag(it.groupValues[1].split(":", limit = 2)) }
            val groups = mutableListOf<CompiledPlaceholder.Holder<*>>()
            tags.forEach { tag ->
                groups.add(entries[tag.key]?.holder(tag, original, projects)?:return@forEach)
            }
            return groups
        }

        /**
         * すべてのプレースホルダーをコンパイルします。
         */
        fun compileAll(original: Project.Original.Json, projects: List<Project.Json>) {
            val holders = this.holders(original, projects)
            original.regexes.placeholder(_InternalCompiler) { _InternalCompiler.compile(holders, original) }
            entries.forEach { placeholder ->
                original.regexes.placeholder(placeholder.value, holders, original)
            }
        }

        /**
         * すべてのプレースホルダーをコンパイルします。
         * 更に引数で指定したプレースホルダも返します。
         */
        fun <T> returningCompileAll(original: Project.Original.Json, projects: List<Project.Json>, placeholder: Placeholder<T>): CompiledPlaceholder<T> {
            this.compileAll(original, projects)
            return original.regexes.placeholder(placeholder) { placeholder.compile(holders(original, projects), original) }
        }

        /**
         * プレースホルダを埋める処理に使います。
         */
        fun on(translation: String, originalText: String, original: Project.Original.Json, projects: List<Project.Json>): String {
            var translated = translation
            entries.values.forEach { placeholder ->
                translated = placeholder.on(translated, originalText, original, projects)
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
            original: Project.Original,
            projects: List<Project.Json>
        ): CompiledPlaceholder.Holder<Nothing> {
            return CompiledPlaceholder.Holder(tag.value!!, this, null)
        }

        override fun on(
            translation: String,
            originalText: String,
            original: Project.Original.Json,
            projects: List<Project.Json>
        ): String {
            val regex = original.regexes.placeholder<Nothing>(this) { returningCompileAll(original, projects, this) }.regex?:return translation
            var translated = translation
            val matches = regex.findAll(originalText)
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
            original: Project.Original,
            projects: List<Project.Json>
        ): CompiledPlaceholder.Holder<Nothing> {
            return CompiledPlaceholder.Holder(Minecraft.getInstance().user.name, this, null)
        }

        override fun on(
            translation: String,
            originalText: String,
            original: Project.Original.Json,
            projects: List<Project.Json>
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
            original: Project.Original,
            projects: List<Project.Json>
        ): CompiledPlaceholder.Holder<Translator.FilterValue> {
            val project = tag.value!!
            // |区切りの値一覧
            val grouping = Translator.filtered(projects.asSequence(), Translator.FilterValue(Component.text(project), null, null))
                .mapNotNull { it.regexes.placeholder<Nothing>(_InternalCompiler) { _InternalCompiler.compile(holders(it, projects), it) }.regex?.pattern }
                .map(::escapeGroupingRegex)
                .joinToString("|")
            return CompiledPlaceholder.Holder("(${grouping})", this, Translator.FilterValue(Component.text(project), null, null))
        }

        override fun on(
            translation: String,
            originalText: String,
            original: Project.Original.Json,
            projects: List<Project.Json>
        ): String {
            val placeholder = original.regexes.placeholder(this) { returningCompileAll(original, projects, this) }
            val holders = placeholder.holders
            val regex = placeholder.regex?:return translation
            var translated = translation
            val matches = regex.findAll(originalText)
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
            original: Project.Original,
            projects: List<Project.Json>
        ): CompiledPlaceholder.Holder<Nothing> {
            // 使われることはないのでエラー
            throw NotImplementedError()
        }

        override fun on(
            translation: String,
            originalText: String,
            original: Project.Original.Json,
            projects: List<Project.Json>
        ): String {
            // 使われることはないのでエラー
            throw NotImplementedError()
        }

        /**
         * すべてのグループをエスケープする
         */
        override fun compile(holders: List<CompiledPlaceholder.Holder<*>>, original: Project.Original.Json): CompiledPlaceholder<Nothing> {
            val patternStr = buildString {
                Placeholder.TAG_PATTERN.split(original.text.value).forEachIndexed { index, str ->
                    append(escapeRegex(str))
                    val group = holders.getOrNull(index)
                    val pattern = group?.pattern
                    if(group != null && pattern != null) {
                        append(escapeGroupingRegex(pattern))
                    }
                }
            }
            return CompiledPlaceholder(patternStr.toRegex(), emptyList())
        }
    }


}