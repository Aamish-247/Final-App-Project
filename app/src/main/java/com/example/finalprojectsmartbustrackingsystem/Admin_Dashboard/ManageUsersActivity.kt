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

        // Buttons ko XML IDs ke sath link karna
        val btnManageDrivers = findViewById<MaterialButton>(R.id.btn_manage_drivers)
        val btnManageParents = findViewById<MaterialButton>(R.id.btn_manage_parents)
        val btnAssignStudents = findViewById<MaterialButton>(R.id.btn_assign_students)

        // 1. Manage Drivers Button
        btnManageDrivers.setOnClickListener {
            val intent = Intent(this, ManageDriversActivity::class.java)
            startActivity(intent)
        }

        // 2. Manage Parents Button
        btnManageParents.setOnClickListener {
            val intent = Intent(this, ManageParentsActivity::class.java)
            startActivity(intent)
        }

        // 3. Assign Students Button
        btnAssignStudents.setOnClickListener {
            val intent = Intent(this, AssignedStudentsListActivity::class.java)
            startActivity(intent)
        }
    }
}