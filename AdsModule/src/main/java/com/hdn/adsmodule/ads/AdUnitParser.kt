package com.hdn.adsmodule.ads

import android.util.Log
import com.google.gson.Gson

object AdUnitParser {
    private const val TAG = "AdUnitParser"
    private val gson = Gson()

    fun parse(raw: String, fallback: String): List<String> {
        Log.d(TAG, "parse: $raw")
        val source = raw.trim()
        if (source.isEmpty()) return listOf(fallback)

        val values = runCatching {
            gson.fromJson(source, Array<String>::class.java)
                ?.map { it.trim() }
                .orEmpty()
        }.getOrDefault(emptyList())

        return values.ifEmpty {
            listOf(source).filter { it.isNotEmpty() }.ifEmpty { listOf(fallback) }
        }
    }
}
