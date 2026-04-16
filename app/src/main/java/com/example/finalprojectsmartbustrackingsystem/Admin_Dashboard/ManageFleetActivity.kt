package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton

class ManageFleetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_fleet)

        // 5 Buttons Initialization
        val btnManageBuses = findViewById<MaterialButton>(R.id.btn_menu_add_bus)
        val btnDefineRoute = findViewById<MaterialButton>(R.id.btn_menu_define_route)
        val btnAssignDriver = findViewById<MaterialButton>(R.id.btn_menu_assign_driver)
        val btnAssignStops = findViewById<MaterialButton>(R.id.btn_assign_student_stops)

        // 1. Manage Buses
        btnManageBuses.setOnClickListener {
            val intent = Intent(this, BusListActivity::class.java)
            startActivity(intent)
        }

        // 2. Define Map Route
        btnDefineRoute.setOnClickListener {
            val intent = Intent(this, DefineRouteActivity::class.java)
            startActivity(intent)
        }

        // 3. Assign Driver to Bus
        btnAssignDriver.setOnClickListener {
            val intent = Intent(this, AssignDriverActivity::class.java)
            startActivity(intent)
        }

        // 5. Assign Stops to Students (NEW BUTTON)
        btnAssignStops.setOnClickListener {
            val intent = Intent(this, AssignPointsActivity::class.java)
            startActivity(intent)
        }
    }
}