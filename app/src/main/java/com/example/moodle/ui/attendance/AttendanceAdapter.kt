package com.example.moodle.ui.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moodle.R

class AttendanceAdapter(
    private val items: MutableList<StudentAttendanceUiModel>,
    private val onStatusChanged: (StudentAttendanceUiModel, AttendanceStatus) -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_student, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(items[position], onStatusChanged)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<StudentAttendanceUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun snapshot(): List<StudentAttendanceUiModel> = items.map { it.copy() }

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvStudentName)
        private val tvId: TextView = itemView.findViewById(R.id.tvStudentId)
        private val radioGroup: RadioGroup = itemView.findViewById(R.id.rgAttendance)
        fun bind(
            item: StudentAttendanceUiModel,
            onStatusChanged: (StudentAttendanceUiModel, AttendanceStatus) -> Unit
        ) {
            tvName.text = item.name
            tvId.text = itemView.context.getString(R.string.attendance_student_id_format, item.id)

            radioGroup.setOnCheckedChangeListener(null)
            when (item.status) {
                AttendanceStatus.PRESENT -> radioGroup.check(R.id.rbPresent)
                AttendanceStatus.ABSENT -> radioGroup.check(R.id.rbAbsent)
                else -> radioGroup.clearCheck()
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.rbPresent -> {
                        if (item.status != AttendanceStatus.PRESENT) {
                            item.status = AttendanceStatus.PRESENT
                            onStatusChanged(item, AttendanceStatus.PRESENT)
                        }
                    }
                    R.id.rbAbsent -> {
                        if (item.status != AttendanceStatus.ABSENT) {
                            item.status = AttendanceStatus.ABSENT
                            onStatusChanged(item, AttendanceStatus.ABSENT)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}
