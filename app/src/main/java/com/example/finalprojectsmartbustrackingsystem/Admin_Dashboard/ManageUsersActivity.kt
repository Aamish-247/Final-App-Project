package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton

class ManageUsersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)


        val btnManageDrivers = findViewById<MaterialButton>(R.id.btn_manage_drivers)
        val btnManageParents = findViewById<MaterialButton>(R.id.btn_manage_parents)
        val btnAssignStudents = findViewById<MaterialButton>(R.id.btn_assign_students)


        btnManageDrivers.setOnClickListener {
            val intent = Intent(this, ManageDriversActivity::class.java)
            startActivity(intent)
        }


        btnManageParents.setOnClickListener {
            val intent = Intent(this, ManageParentsActivity::class.java)
            startActivity(intent)
        }


        btnAssignStudents.setOnClickListener {
            val intent = Intent(this, AssignedStudentsListActivity::class.java)
            startActivity(intent)
        }
    }
}