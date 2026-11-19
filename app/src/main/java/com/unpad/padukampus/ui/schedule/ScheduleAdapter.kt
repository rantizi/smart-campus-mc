package com.unpad.padukampus.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unpad.padukampus.R
import com.unpad.padukampus.data.model.JenisPerubahan
import com.unpad.padukampus.data.repository.KelasHarian
import com.unpad.padukampus.databinding.ItemScheduleBinding

class ScheduleAdapter(
    private val edit: (KelasHarian) -> Unit,
    private val recordChange: (KelasHarian) -> Unit,
    private val delete: (KelasHarian) -> Unit,
) : ListAdapter<KelasHarian, ScheduleAdapter.Holder>(Diff) {
    class Holder(val ui: ItemScheduleBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val context = holder.ui.root.context
        with(holder.ui) {
            title.text = item.mataKuliah
            time.text =
                context.getString(
                    R.string.schedule_time_range,
                    item.jamMulai.toString(),
                    item.jamSelesai.toString(),
                )
            location.text = item.lokasiLabel
            lecturer.isVisible = item.dosen.isNotBlank()
            lecturer.text = context.getString(R.string.schedule_lecturer, item.dosen)
            val (label, color) =
                when {
                    item.dibatalkan -> R.string.schedule_cancelled to R.color.pk_galat
                    item.jenisPerubahan == JenisPerubahan.PINDAH_RUANG ->
                        R.string.schedule_moved to R.color.pk_primer
                    item.jenisPerubahan == JenisPerubahan.GANTI_JAM ->
                        R.string.schedule_time_changed to R.color.pk_primer
                    else -> null to R.color.pk_teks_sekunder
                }
            val note =
                listOfNotNull(label?.let { context.getString(it) }, item.catatan.ifBlank { null })
            status.isVisible = note.isNotEmpty()
            status.text = note.joinToString(" · ")
            status.setTextColor(ContextCompat.getColor(context, color))
            menu.setOnClickListener { anchor ->
                PopupMenu(context, anchor).apply {
                    this.menu.add(0, 1, 0, R.string.schedule_edit)
                    this.menu.add(0, 2, 1, R.string.schedule_record_change)
                    this.menu.add(0, 3, 2, R.string.schedule_delete)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            1 -> edit(item)
                            2 -> recordChange(item)
                            else -> delete(item)
                        }
                        true
                    }
                    show()
                }
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<KelasHarian>() {
        override fun areItemsTheSame(oldItem: KelasHarian, newItem: KelasHarian) =
            oldItem.id == newItem.id && oldItem.tanggal == newItem.tanggal

        override fun areContentsTheSame(oldItem: KelasHarian, newItem: KelasHarian) =
            oldItem == newItem
    }
}
