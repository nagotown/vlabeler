package com.sdercolin.vlabeler.ui.string

var currentLanguage: Language = Language.English

enum class Language(val code: String, val displayName: String) : Text {
    English("en", "English"),
    ChineseSimplified("zh-Hans", "简体中文"),
    ;

    override val text: String
        get() = displayName

    companion object {

        fun find(languageTag: String): Language? {
            return values().find { languageTag.startsWith(it.code) }
        }
    }
}
