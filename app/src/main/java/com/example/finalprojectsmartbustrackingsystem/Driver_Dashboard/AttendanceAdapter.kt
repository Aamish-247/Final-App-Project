package com.example.finalprojectsmartbustrackingsystem.Driver_Dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalprojectsmartbustrackingsystem.R
import com.example.finalprojectsmartbustrackingsystem.StudentModel
import com.google.firebase.database.FirebaseDatabase

class AttendanceAdapter(private val studentList: ArrayList<StudentModel>) :
    RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_student_name)
        val tvStops: TextView = itemView.findViewById(R.id.tv_stops)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_current_status)
        val btnPick: Button = itemView.findViewById(R.id.btn_pick)
        val btnDrop: Button = itemView.findViewById(R.id.btn_drop)
        val btnAbsent: Button = itemView.findViewById(R.id.btn_absent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = studentList[position]

        holder.tvName.text = student.studentName
        holder.tvStops.text = "Pickup: ${student.pickupStop} | Dropoff: ${student.dropoffStop}"
        holder.tvStatus.text = "Status: ${student.attendanceStatus}"

        // UI Colors based on status
        when (student.attendanceStatus) {
            "Picked Up" -> holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
            "Dropped Off" -> holder.tvStatus.setTextColor(Color.parseColor("#2196F3")) // Blue
            "Absent" -> holder.tvStatus.setTextColor(Color.parseColor("#F44336")) // Red
            else -> holder.tvStatus.setTextColor(Color.parseColor("#FF9800")) // Orange for Pending
        }

        // Database Update Logic
        val dbRef = FirebaseDatabase.getInstance().getReference("students").child(student.studentId!!)

        holder.btnPick.setOnClickListener {
            dbRef.child("attendanceStatus").setValue("Picked Up")
        }

        holder.btnDrop.setOnClickListener {
            dbRef.child("attendanceStatus").setValue("Dropped Off")
        }

        holder.btnAbsent.setOnClickListener {
            dbRef.child("attendanceStatus").setValue("Absent")
        }
    }

    override fun getItemCount(): Int {
        return studentList.size
    }
}