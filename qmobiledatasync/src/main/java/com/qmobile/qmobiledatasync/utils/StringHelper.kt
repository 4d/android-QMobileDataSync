/*
 * Created by qmarciset on 17/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

@file:Suppress("TooManyFunctions")

package com.qmobile.qmobiledatasync.utils

import java.text.Normalizer

fun String.containsIgnoreCase(str: String): Boolean =
    this.lowercase().contains(str.lowercase())

fun String.tableNameAdjustment() =
    this.condense().replaceFirstChar { it.uppercaseChar() }.replaceSpecialChars().firstCharForTable()
        .validateWord()

fun String.fieldAdjustment() =
    this.condense().replaceSpecialChars().lowerCustomProperties().validateWordDecapitalized()

private fun String.condense() =
    if (!this.startsWith("Map<")) {
        this.replace("\\s".toRegex(), "")
    } else {
        this
    }

private fun String.replaceSpecialChars(): String =
    when {
        this.contains("Entities<") -> this.unaccent().replace("[^a-zA-Z0-9._<>]".toRegex(), "_")
        this.contains("Map<") -> this.unaccent().replace("[^a-zA-Z0-9._<>, ]".toRegex(), "_")
        else -> this.unaccent().replace("[^a-zA-Z0-9._]".toRegex(), "_")
    }

private fun String.lowerCustomProperties() = when {
    this in arrayOf("__KEY", "__STAMP", "__GlobalStamp", "__TIMESTAMP") -> this
    this.startsWith("__") && this.endsWith("Key") ->
        this.removeSuffix("Key").replaceFirstChar { it.lowercaseChar() } + "Key"
    this == "ID" -> this
    else -> this.replaceFirstChar { it.lowercaseChar() }
}

private fun String.decapitalizeExceptID() =
    if (this == "ID") this else this.replaceFirstChar { it.lowercaseChar() }

private fun String.firstCharForTable(): String =
    if (this.startsWith("_")) {
        "Q$this"
    } else {
        this
    }

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

private fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

fun String.validateWord(): String {
    return this.split(".").joinToString(".") {
        if (reservedKeywords.contains(it)) "qmobile_$it" else it
    }
}

fun String.validateWordDecapitalized(): String {
    return this.decapitalizeExceptID().split(".").joinToString(".") {
        when {
            reservedKeywords.contains(it) -> "qmobile_$it"
            it == "ID" -> "__ID"
            else -> it
        }
    }
}

val reservedKeywords = listOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",
    "by",
    "catch",
    "constructor",
    "delegate",
    "dynamic",
    "field",
    "file",
    "finally",
    "get",
    "import",
    "init",
    "param",
    "property",
    "receiver",
    "set",
    "setparam",
    "where",
    "actual",
    "abstract",
    "annotation",
    "companion",
    "const",
    "crossinline",
    "data",
    "enum",
    "expect",
    "external",
    "final",
    "infix",
    "inline",
    "inner",
    "internal",
    "lateinit",
    "noinline",
    "open",
    "operator",
    "out",
    "override",
    "private",
    "protected",
    "public",
    "reified",
    "sealed",
    "suspend",
    "tailrec",
    "vararg",
    "field",
    "it"
)
