package com.example.finalprojectsmartbustrackingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class AdminDashboard : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)


        val btnUser = findViewById<MaterialButton>(R.id.btn_manage_users)
        val btnBus = findViewById<MaterialButton>(R.id.btn_manage_fleet)
        val btnMap = findViewById<MaterialButton>(R.id.btn_live_tracking)
        val btnNotif = findViewById<MaterialButton>(R.id.btn_send_alerts)
        val btnLogout = findViewById<MaterialButton>(R.id.btn_logout)

        // 1. User Management (Sub-menu par le kar jayega)
        btnUser.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            startActivity(intent)
        }

        // 2. Manage Fleet (Abhi features pending hain)
        btnBus.setOnClickListener {
            Toast.makeText(this, "Fleet Management Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // 3. Live Tracking (Map functionality pending)
        btnMap.setOnClickListener {
            Toast.makeText(this, "Live Tracking Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // 4. Notifications/Alerts (Pending)
        btnNotif.setOnClickListener {
            Toast.makeText(this, "Alerts System Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // 5. Logout Functionality
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Firebase session khatam
            val intent = Intent(this, LoginActivity::class.java)
            // Is se pichli saari screens clear ho jayengi taake user back na aa sakay
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
        }
    }
}