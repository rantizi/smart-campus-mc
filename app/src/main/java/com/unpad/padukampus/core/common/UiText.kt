package com.unpad.padukampus.core.common

import android.content.Context
import androidx.annotation.StringRes

/**
 * Teks untuk UI tanpa Context di ViewModel: pesan milik aplikasi memakai resource string
 * (dapat diterjemahkan), pesan validasi dari repository dibawa apa adanya.
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int) : UiText

    data class Raw(val value: String) : UiText

    fun asString(context: Context): String =
        when (this) {
            is Res -> context.getString(id)
            is Raw -> value
        }

    companion object {
        /** Pesan dari exception validasi (require) atau [fallback] bila kosong. */
        fun from(error: Throwable, @StringRes fallback: Int): UiText =
            error.message?.takeIf { it.isNotBlank() }?.let(::Raw) ?: Res(fallback)
    }
}
