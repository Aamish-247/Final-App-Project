package com.example.finalprojectsmartbustrackingsystem.Driver_Dashboard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalprojectsmartbustrackingsystem.R
import com.example.finalprojectsmartbustrackingsystem.StudentModel
import com.google.firebase.database.*

class DriverAttendanceActivity : AppCompatActivity() {

    private lateinit var rvAttendance: RecyclerView
    private lateinit var studentList: ArrayList<StudentModel>
    private lateinit var adapter: AttendanceAdapter
    private lateinit var dbRef: DatabaseReference

    private var currentBusId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_attendance)

        rvAttendance = findViewById(R.id.rv_attendance)
        rvAttendance.layoutManager = LinearLayoutManager(this)

        studentList = ArrayList()
        adapter = AttendanceAdapter(studentList)
        rvAttendance.adapter = adapter

        currentBusId = intent.getStringExtra("BUS_ID") ?: ""

        if (currentBusId.isNotEmpty()) {
            fetchStudentsForThisBus()
        } else {
            Toast.makeText(this, "Bus ID not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchStudentsForThisBus() {
        dbRef = FirebaseDatabase.getInstance().getReference("students")

        val query = dbRef.orderByChild("busId").equalTo(currentBusId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                studentList.clear()
                if (snapshot.exists()) {
                    for (studentSnap in snapshot.children) {
                        val student = studentSnap.getValue(StudentModel::class.java)
                        student?.let { studentList.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@DriverAttendanceActivity, "No students found for this bus", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriverAttendanceActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}