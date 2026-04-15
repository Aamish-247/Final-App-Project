package com.example.finalprojectsmartbustrackingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ManageFleetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_fleet)

        val btnManageBuses = findViewById<MaterialButton>(R.id.btn_menu_add_bus)
        val btnDefineRoute = findViewById<MaterialButton>(R.id.btn_menu_define_route)
        val btnAssignDriver = findViewById<MaterialButton>(R.id.btn_menu_assign_driver)
        val btnSchedule = findViewById<MaterialButton>(R.id.btn_menu_schedule)

        // 1. Add New Bus
        // 1. Manage Buses (Ab ye List wale page par le kar jayega)
        btnManageBuses.setOnClickListener {
            val intent = Intent(this, BusListActivity::class.java)
            startActivity(intent)
        }

        // 2. Define Map Route
        btnDefineRoute.setOnClickListener {
            Toast.makeText(this, "Opening Map Route Designer...", Toast.LENGTH_SHORT).show()
        }

        // 3. Assign Driver to Bus
        btnAssignDriver.setOnClickListener {
            Toast.makeText(this, "Opening Driver Assignment...", Toast.LENGTH_SHORT).show()
        }

        // 4. Timings & Schedule
        btnSchedule.setOnClickListener {
            Toast.makeText(this, "Opening Schedule Manager...", Toast.LENGTH_SHORT).show()
        }
    }
}