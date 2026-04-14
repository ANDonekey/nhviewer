package com.nhviewer.data.local.bootstrap

import android.content.Context
import android.util.Log
import com.nhviewer.data.local.dao.TagDao
import com.nhviewer.data.local.entity.TagEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.Locale

class TagCatalogBootstrapper(
    private val context: Context,
    private val tagDao: TagDao
) {

    suspend fun ensureSeeded() {
        val total = tagDao.count()
        val translated = tagDao.countWithChineseName()
        if (total > 0 && translated > 0) return

        runCatching {
            val seedRows = readSeedRows()
            if (total <= 0) {
                seedRows.chunked(500).forEach { chunk ->
                    tagDao.upsertAll(chunk)
                }
                Log.i(TAG, "Seeded local tags from asset: ${seedRows.size}")
            }

            if (translated <= 0) {
                val updated = applyPlainTranslationsToLocalTags()
                Log.i(TAG, "Backfilled translated tag names from plain asset: $updated")
            }
        }.onFailure {
            Log.e(TAG, "Failed to initialize local tag catalog", it)
        }
    }

    @Serializable
    private data class TagSeedRecord(
        val id: Long,
        val type: String,
        val name: String,
        val slug: String,
        val count: Int? = 0,
        val updated_at: Long? = null
    )

    @Serializable
    private data class PlainTranslationRecord(
        val key: String,
        val zh: String
    )

    private fun readSeedRows(): List<TagEntity> {
        val content = context.assets.open(TAG_SEED_ASSET).bufferedReader().use { it.readText() }
        return Json { ignoreUnknownKeys = true }
            .decodeFromString(ListSerializer(TagSeedRecord.serializer()), content)
            .mapNotNull { row ->
                if (row.id <= 0L || row.type.isBlank() || row.name.isBlank() || row.slug.isBlank()) {
                    null
                } else {
                    TagEntity(
                        id = row.id,
                        type = row.type,
                        name = row.name,
                        slug = row.slug,
                        // Explicitly keep translation source isolated in plain translation asset.
                        nameZh = null,
                        count = row.count ?: 0,
                        updatedAt = row.updated_at ?: System.currentTimeMillis()
                    )
                }
            }
    }

    private suspend fun applyPlainTranslationsToLocalTags(): Int {
        val translations = readPlainTranslations()
        if (translations.isEmpty()) return 0

        val byKey = translations.associate { it.key.lowercase(Locale.ROOT) to it.zh }
        val uniqueSuffixMap = translations
            .groupBy { it.key.substringAfter(':', it.key).lowercase(Locale.ROOT) }
            .mapNotNull { (suffix, list) ->
                val distinct = list.map { it.zh }.distinct()
                if (distinct.size == 1) suffix to distinct.first() else null
            }
            .toMap()

        val now = System.currentTimeMillis()
        val current = tagDao.getAll()
        if (current.isEmpty()) return 0

        val updatedRows = current.mapNotNull { tag ->
            val translated = resolveTranslation(tag, byKey, uniqueSuffixMap) ?: return@mapNotNull null
            if (tag.nameZh == translated) return@mapNotNull null
            tag.copy(nameZh = translated, updatedAt = now)
        }

        if (updatedRows.isNotEmpty()) {
            updatedRows.chunked(500).forEach { chunk ->
                tagDao.upsertAll(chunk)
            }
        }
        return updatedRows.size
    }

    private fun readPlainTranslations(): List<PlainTranslationRecord> {
        val content = context.assets.open(TAG_TRANSLATION_ASSET).bufferedReader().use { it.readText() }
        return Json { ignoreUnknownKeys = true }
            .decodeFromString(ListSerializer(PlainTranslationRecord.serializer()), content)
            .filter { it.key.isNotBlank() && it.zh.isNotBlank() }
    }

    private fun resolveTranslation(
        tag: TagEntity,
        byKey: Map<String, String>,
        uniqueSuffixMap: Map<String, String>
    ): String? {
        val type = tag.type.lowercase(Locale.ROOT)
        val name = normalize(tag.name)
        val slug = normalize(tag.slug.replace('-', ' '))
        val prefix = TYPE_PREFIX[type]

        val directKeys = mutableListOf<String>()
        if (prefix != null) {
            directKeys += prefix + name
            directKeys += prefix + slug
        } else {
            directKeys += name
            directKeys += slug
        }

        directKeys.firstNotNullOfOrNull { key -> byKey[key] }?.let { return it }
        uniqueSuffixMap[name]?.let { return it }
        uniqueSuffixMap[slug]?.let { return it }
        return null
    }

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)

    companion object {
        private const val TAG = "TagCatalogBootstrapper"
        private const val TAG_SEED_ASSET = "tag_catalog_seed.json"
        private const val TAG_TRANSLATION_ASSET = "tag_translation_plain_zh_cn.json"

        private val TYPE_PREFIX = mapOf(
            "artist" to "a:",
            "character" to "c:",
            "group" to "g:",
            "language" to "l:",
            "parody" to "p:"
        )
    }
}
