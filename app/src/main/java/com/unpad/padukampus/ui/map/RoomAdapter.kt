package com.unpad.padukampus.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unpad.padukampus.R
import com.unpad.padukampus.data.local.relation.RuanganDetail
import com.unpad.padukampus.databinding.ItemRoomBinding

/** Hasil pencarian ruangan (FR-09); ketuk untuk menampilkan lokasinya di peta. */
class RoomAdapter(private val select: (RuanganDetail) -> Unit) :
    ListAdapter<RuanganDetail, RoomAdapter.Holder>(Diff) {
    class Holder(val ui: ItemRoomBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.ui.label.text =
            holder.ui.root.context.getString(
                R.string.map_room_item,
                item.ruangan.nama,
                item.ruangan.kode,
                item.ruangan.lantai,
            )
        holder.ui.label.setOnClickListener { select(item) }
    }

    private object Diff : DiffUtil.ItemCallback<RuanganDetail>() {
        override fun areItemsTheSame(oldItem: RuanganDetail, newItem: RuanganDetail) =
            oldItem.ruangan.id == newItem.ruangan.id

        override fun areContentsTheSame(oldItem: RuanganDetail, newItem: RuanganDetail) =
            oldItem == newItem
    }
}
