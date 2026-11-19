package com.unpad.padukampus.data.remote.sync

import com.unpad.padukampus.core.common.AppResult

/** Week 3 integration boundary. No Firebase login or synchronization is implemented. */
interface CloudSyncManager {
    suspend fun sync(): AppResult<Unit>
}

class NotConfiguredCloudSyncManager : CloudSyncManager {
    override suspend fun sync(): AppResult<Unit> =
        AppResult.Error(
            "Sinkronisasi Firebase belum diimplementasikan. Data masih tersimpan lokal."
        )
}
