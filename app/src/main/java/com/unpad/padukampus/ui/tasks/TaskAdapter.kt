package com.unpad.padukampus.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unpad.padukampus.R
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.local.relation.TugasDetail
import com.unpad.padukampus.databinding.ItemTaskBinding

class TaskAdapter(
    private val edit: (String) -> Unit,
    private val toggle: (String, Boolean) -> Unit,
    private val delete: (String) -> Unit,
) : ListAdapter<TugasDetail, TaskAdapter.Holder>(Diff) {
    class Holder(val ui: ItemTaskBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val task = item.tugas
        with(holder.ui) {
            title.text = task.judul
            course.text =
                root.context.getString(
                    R.string.task_course_source,
                    item.mataKuliah.nama,
                    task.sumber,
                )
            deadline.text = DateTimeUtils.formatDeadline(task.deadline)
            // Tabel 5 Laporan 1: Galat = tugas terlambat, Berhasil = tugas selesai.
            val overdue = !task.selesai && task.deadline < System.currentTimeMillis()
            if (overdue)
                deadline.text =
                    root.context.getString(R.string.task_overdue, deadline.text.toString())
            deadline.setTextColor(
                ContextCompat.getColor(
                    root.context,
                    when {
                        task.selesai -> R.color.pk_berhasil
                        overdue -> R.color.pk_galat
                        else -> R.color.pk_teks_sekunder
                    },
                )
            )
            done.setOnCheckedChangeListener(null)
            done.isChecked = task.selesai
            done.setOnCheckedChangeListener { _, checked -> toggle(task.id, checked) }
            title.setOnClickListener { edit(task.id) }
            remove.contentDescription =
                root.context.getString(R.string.task_delete_named, task.judul)
            remove.setOnClickListener { delete(task.id) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TugasDetail>() {
        override fun areItemsTheSame(oldItem: TugasDetail, newItem: TugasDetail) =
            oldItem.tugas.id == newItem.tugas.id

        override fun areContentsTheSame(oldItem: TugasDetail, newItem: TugasDetail) =
            oldItem == newItem
    }
}
