package com.unpad.padukampus.data.local.dao

import androidx.room.*
import com.unpad.padukampus.data.local.entity.CacheCuacaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CuacaDao {
    @Query("SELECT * FROM cache_cuaca WHERE id = :key LIMIT 1")
    fun observeCache(key: String): Flow<CacheCuacaEntity?>

    @Query("SELECT * FROM cache_cuaca WHERE id = :key LIMIT 1")
    suspend fun getCache(key: String): CacheCuacaEntity?

    @Upsert suspend fun simpanCache(value: CacheCuacaEntity)
}
