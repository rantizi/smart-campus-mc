package com.unpad.padukampus.data.local.dao

import androidx.room.*
import com.unpad.padukampus.data.local.entity.GedungEntity
import com.unpad.padukampus.data.local.entity.RuanganEntity
import com.unpad.padukampus.data.local.relation.RuanganDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface KampusDao {
    @Query("SELECT * FROM gedung ORDER BY nama") fun observeGedung(): Flow<List<GedungEntity>>

    @Transaction
    @Query(
        "SELECT * FROM ruangan WHERE kode LIKE '%' || :search || '%' OR nama LIKE '%' || :search || '%' ORDER BY kode"
    )
    fun observeRuangan(search: String): Flow<List<RuanganDetail>>

    @Query("SELECT * FROM ruangan WHERE id = :id")
    suspend fun getRuangan(id: String): RuanganEntity?

    @Upsert suspend fun simpanGedung(gedung: GedungEntity)

    @Upsert suspend fun simpanRuangan(ruangan: RuanganEntity)
}
