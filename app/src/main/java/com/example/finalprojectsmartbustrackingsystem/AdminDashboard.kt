package com.example.finalprojectsmartbustrackingsystem

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AdminDashboard : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Sirf check karne ke liye ke buttons kaam kar rahe hain
        val btnUser = findViewById<MaterialButton>(R.id.btn_manage_users)
        val btnBus = findViewById<MaterialButton>(R.id.btn_manage_fleet)
        val btnMap = findViewById<MaterialButton>(R.id.btn_live_tracking)
        val btnNotif = findViewById<MaterialButton>(R.id.btn_send_alerts)
        val btnLogout = findViewById<MaterialButton>(R.id.btn_logout)

        btnUser.setOnClickListener { Toast.makeText(this, "User Clicked", Toast.LENGTH_SHORT).show() }
        btnBus.setOnClickListener { Toast.makeText(this, "Bus Clicked", Toast.LENGTH_SHORT).show() }
        btnMap.setOnClickListener { Toast.makeText(this, "Map Clicked", Toast.LENGTH_SHORT).show() }
        btnNotif.setOnClickListener { Toast.makeText(this, "Notification Clicked", Toast.LENGTH_SHORT).show() }

        btnLogout.setOnClickListener {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
            finish() // Ye aapko wapis login screen par le jayega
        }
    }
}