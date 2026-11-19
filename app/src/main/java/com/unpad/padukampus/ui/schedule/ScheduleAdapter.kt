package com.unpad.padukampus.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unpad.padukampus.R
import com.unpad.padukampus.data.repository.KelasHarian
import com.unpad.padukampus.databinding.ItemScheduleBinding

class ScheduleAdapter : ListAdapter<KelasHarian, ScheduleAdapter.Holder>(Diff) {
    class Holder(val ui: ItemScheduleBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.ui) {
            title.text = item.mataKuliah
            time.text =
                root.context.getString(
                    R.string.schedule_time_range,
                    item.jamMulai.toString(),
                    item.jamSelesai.toString(),
                )
            location.text = item.lokasiLabel
            status.text =
                if (item.dibatalkan) root.context.getString(R.string.schedule_cancelled)
                else item.catatan
        }
    }

    private object Diff : DiffUtil.ItemCallback<KelasHarian>() {
        override fun areItemsTheSame(oldItem: KelasHarian, newItem: KelasHarian) =
            oldItem.id == newItem.id && oldItem.tanggal == newItem.tanggal

        override fun areContentsTheSame(oldItem: KelasHarian, newItem: KelasHarian) =
            oldItem == newItem
    }
}
