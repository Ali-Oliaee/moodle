package com.example.moodle.ui.participants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moodle.R
import com.example.moodle.data.model.AttendanceStatus
import com.example.moodle.data.model.Participant

class ParticipantAdapter(
    private val participants: List<Participant>,
    private val onAttendanceChanged: (Participant, AttendanceStatus) -> Unit
) : RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder>() {

    inner class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvParticipantName)
        private val tvId: TextView = itemView.findViewById(R.id.tvParticipantId)
        private val btnPresent: Button = itemView.findViewById(R.id.btnPresent)
        private val btnAbsent: Button = itemView.findViewById(R.id.btnAbsent)
        private val btnLate: Button = itemView.findViewById(R.id.btnLate)
        private val btnExcused: Button = itemView.findViewById(R.id.btnExcused)

        fun bind(participant: Participant) {
            tvName.text = participant.name
            tvId.text = participant.id

            // Reset button states
            resetButtonStates()

            // Highlight selected status
            when (participant.attendanceStatus) {
                AttendanceStatus.PRESENT -> highlightButton(btnPresent)
                AttendanceStatus.ABSENT -> highlightButton(btnAbsent)
                AttendanceStatus.LATE -> highlightButton(btnLate)
                AttendanceStatus.EXCUSED -> highlightButton(btnExcused)
                AttendanceStatus.NONE -> {}
            }

            // Set click listeners
            btnPresent.setOnClickListener {
                updateAttendance(participant, AttendanceStatus.PRESENT)
            }

            btnAbsent.setOnClickListener {
                updateAttendance(participant, AttendanceStatus.ABSENT)
            }

            btnLate.setOnClickListener {
                updateAttendance(participant, AttendanceStatus.LATE)
            }

            btnExcused.setOnClickListener {
                updateAttendance(participant, AttendanceStatus.EXCUSED)
            }
        }

        private fun updateAttendance(participant: Participant, status: AttendanceStatus) {
            participant.attendanceStatus = status
            onAttendanceChanged(participant, status)
            notifyItemChanged(adapterPosition)
        }

        private fun resetButtonStates() {
            btnPresent.alpha = 0.5f
            btnAbsent.alpha = 0.5f
            btnLate.alpha = 0.5f
            btnExcused.alpha = 0.5f
        }

        private fun highlightButton(button: Button) {
            button.alpha = 1.0f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(participants[position])
    }

    override fun getItemCount(): Int = participants.size
}

