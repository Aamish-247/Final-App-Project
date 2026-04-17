package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class BroadcastAlertsActivity : AppCompatActivity() {

    private lateinit var spinnerType: AutoCompleteTextView
    private lateinit var spinnerAudience: AutoCompleteTextView
    private lateinit var etMessage: TextInputEditText
    private lateinit var btnSend: MaterialButton

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast_alerts)

        // XML Views ko link karna
        spinnerType = findViewById(R.id.spinner_alert_type)
        spinnerAudience = findViewById(R.id.spinner_target_audience)
        etMessage = findViewById(R.id.et_alert_message)
        btnSend = findViewById(R.id.btn_send_alert)

        // Firebase Node: "broadcast_alerts"
        dbRef = FirebaseDatabase.getInstance().getReference("broadcast_alerts")

        // Dropdowns mein data dalna
        setupDropdowns()

        // Send Button Click
        btnSend.setOnClickListener {
            sendAlertToDatabase()
        }
    }

    private fun setupDropdowns() {
        // Alert Category Options
        val alertTypes = arrayOf("Emergency", "Holiday", "Maintenance", "Delays")
        val adapterType = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, alertTypes)
        spinnerType.setAdapter(adapterType)

        // Target Audience Options
        val audiences = arrayOf("All", "Drivers Only", "Parents Only")
        val adapterAudience = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, audiences)
        spinnerAudience.setAdapter(adapterAudience)
    }

    private fun sendAlertToDatabase() {
        val type = spinnerType.text.toString().trim()
        val audience = spinnerAudience.text.toString().trim()
        val message = etMessage.text.toString().trim()

        // Validation: Koi field khali na ho
        if (type.isEmpty() || audience.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase mein ek nayi unique ID banana is alert ke liye
        val alertId = dbRef.push().key ?: return

        // System ka maujooda waqt (taake app mein time dikhaya ja sake)
        val timestamp = System.currentTimeMillis()

        // Data ko HashMap mein pack karna
        val alertData = HashMap<String, Any>()
        alertData["alertId"] = alertId
        alertData["type"] = type
        alertData["audience"] = audience
        alertData["message"] = message
        alertData["timestamp"] = timestamp

        // Firebase par save karna
        dbRef.child(alertId).setValue(alertData)
            .addOnSuccessListener {
                Toast.makeText(this, "Alert Sent Successfully!", Toast.LENGTH_LONG).show()

                // Form ko wapis khali kar dena
                etMessage.text?.clear()
                spinnerType.text.clear()
                spinnerAudience.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}