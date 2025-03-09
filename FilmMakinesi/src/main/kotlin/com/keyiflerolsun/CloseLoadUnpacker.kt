package com.keyiflerolsun

import com.lagradost.cloudstream3.ErrorLoadingException
import kotlin.math.pow

private val packedExtractRegex = Regex(
    """\}\('(.*)',\s*(\d+),\s*(\d+),\s*'(.*?)'\.split\('\|'\)""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
)

private val unpackReplaceRegex = Regex(
    """\b\w+\b""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
)

private data class Unbaser(private val base: Int) {
    private val selector: Int = when {
        base > 62 -> 95
        base > 54 -> 62
        base > 52 -> 54
        else -> 52
    }

    // Cache the mapping of characters to indices
    private val dict: Map<Char, Int>? by lazy {
        ALPHABET[selector]?.withIndex()?.associate { (index, char) -> char to index }
    }

    fun unbase(value: String): Int =
        if (base in 2..36) {
            value.toIntOrNull(base) ?: 0
        } else {
            value.reversed().foldIndexed(0) { index, acc, c ->
                acc + (dict?.get(c) ?: 0) * base.toDouble().pow(index).toInt()
            }
        }

    companion object {
        private val ALPHABET = mapOf(
            52 to "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP",
            54 to "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR",
            62 to "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
            95 to " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
        )
    }
}

internal fun unpack(scriptBlock: String): String =
    packedExtractRegex.find(scriptBlock)!!.destructured.let { (payload, radixStr, countStr, symtabStr) ->
        val radix = radixStr.toInt()
        val count = countStr.toInt()
        val symtab = symtabStr.split('|')
        if (symtab.size != count) throw ErrorLoadingException("there is an error in the packed script")

        val unbaser = Unbaser(radix)
        payload.replace(unpackReplaceRegex) { matchResult ->
            val word = matchResult.value
            val index = unbaser.unbase(word)
            symtab.getOrElse(index) { word }.ifEmpty { word }
        }
    }
