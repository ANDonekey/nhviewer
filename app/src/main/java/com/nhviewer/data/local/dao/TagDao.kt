package com.nhviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nhviewer.data.local.entity.TagEntity

@Dao
interface TagDao {

    @Upsert
    suspend fun upsertAll(tags: List<TagEntity>)

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM tags WHERE name_zh IS NOT NULL AND TRIM(name_zh) <> ''")
    suspend fun countWithChineseName(): Int

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TagEntity>

    @Query("UPDATE tags SET name_zh = :nameZh, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateChineseName(id: Long, nameZh: String?, updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT *
        FROM tags
        WHERE
            (:keyword = '' OR name LIKE '%' || :keyword || '%' OR slug LIKE '%' || :keyword || '%' OR IFNULL(name_zh, '') LIKE '%' || :keyword || '%')
            AND type IN ('artist','category','character','group','language','parody','tag')
        ORDER BY
            CASE WHEN type = 'language' THEN 0 ELSE 1 END,
            name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun searchByKeyword(keyword: String, limit: Int = 50): List<TagEntity>
}
