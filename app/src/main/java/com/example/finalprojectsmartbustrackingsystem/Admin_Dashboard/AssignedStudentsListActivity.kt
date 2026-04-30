package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalprojectsmartbustrackingsystem.R
import com.example.finalprojectsmartbustrackingsystem.StudentModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class AssignedStudentsListActivity : AppCompatActivity() {

    private lateinit var rvStudents: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var studentList: ArrayList<StudentModel>

    private lateinit var studentAdapter: RecyclerView.Adapter<StudentViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assigned_students_list_activity)

        rvStudents = findViewById(R.id.rv_students_list)
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.setHasFixedSize(true)

        studentList = arrayListOf<StudentModel>()
        dbRef = FirebaseDatabase.getInstance().getReference("students")


        initAdapter()

        getStudentsData()

        findViewById<MaterialButton>(R.id.btn_open_assign_form).setOnClickListener {
            startActivity(Intent(this, AssignStudentActivity::class.java))
        }
    }

    private fun initAdapter() {
        studentAdapter = object : RecyclerView.Adapter<StudentViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
                return StudentViewHolder(view)
            }

            override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
                val current = studentList[position]

                holder.name.text = current.studentName ?: "Unknown Student"
                holder.busInfo.text = "Bus: ${current.busName ?: "Not Assigned"}"
                holder.driverName.text = "Driver: ${current.driverName ?: "Not Assigned"}"
                holder.parentName.text = "Parent: ${current.parentName ?: "Not Assigned"}"


                val pickup = current.pickupStop?.replace("_", " ")?.capitalize() ?: "Pending"
                val dropoff = current.dropoffStop?.replace("_", " ")?.capitalize() ?: "Pending"

                holder.pickupInfo.text = "Pickup: $pickup"
                holder.dropoffInfo.text = "Drop-off: $dropoff"



                holder.btnDelete.setOnClickListener {
                    val builder = AlertDialog.Builder(this@AssignedStudentsListActivity)
                    builder.setTitle("Remove Assignment")
                    builder.setMessage("do you want to remove ${current.studentName} ?")
                    builder.setPositiveButton("Yes, Remove") { dialog, _ ->
                        current.studentId?.let { id ->
                            dbRef.child(id).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this@AssignedStudentsListActivity, "Removed Successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@AssignedStudentsListActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        dialog.dismiss()
                    }
                    builder.setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                }
            }

            override fun getItemCount(): Int = studentList.size
        }


        rvStudents.adapter = studentAdapter
    }

    private fun getStudentsData() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                studentList.clear()

                if (snapshot.exists()) {
                    for (studentSnap in snapshot.children) {
                        val data = studentSnap.getValue(StudentModel::class.java)
                        data?.let { studentList.add(it) }
                    }
                }

                studentAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AssignedStudentsListActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_student_name)
        val busInfo: TextView = itemView.findViewById(R.id.tv_student_bus_info)
        val driverName: TextView = itemView.findViewById(R.id.tv_student_driver_name)
        val parentName: TextView = itemView.findViewById(R.id.tv_student_parent_name)

        val pickupInfo: TextView = itemView.findViewById(R.id.tv_student_pickup)
        val dropoffInfo: TextView = itemView.findViewById(R.id.tv_student_dropoff)

        val btnDelete: ImageView = itemView.findViewById(R.id.iv_delete_student)
    }
}