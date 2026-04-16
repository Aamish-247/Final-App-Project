package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*
import java.util.Locale

class AssignPointsActivity : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference

    private lateinit var spinnerStudent: AutoCompleteTextView
    private lateinit var spinnerPickup: AutoCompleteTextView
    private lateinit var spinnerDropoff: AutoCompleteTextView
    private lateinit var tvBusInfo: TextView
    private lateinit var cardBusInfo: View

    private val studentMap = HashMap<String, String>() // Sirf Name aur ID save karenge
    private val routeStopsList = ArrayList<String>()

    private var selectedStudentId: String? = null
    private var fetchedBusId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_points)

        dbRef = FirebaseDatabase.getInstance().reference

        spinnerStudent = findViewById(R.id.spinner_student)
        spinnerPickup = findViewById(R.id.spinner_pickup)
        spinnerDropoff = findViewById(R.id.spinner_dropoff)
        tvBusInfo = findViewById(R.id.tv_auto_fetched_bus)
        cardBusInfo = findViewById(R.id.card_bus_info)

        // Activity start par sirf Students ke naam load honge
        loadOnlyStudents()

        // === STEP 1: Admin dropdown se student select karega ===
        spinnerStudent.setOnItemClickListener { _, _, position, _ ->
            val selectedName = spinnerStudent.adapter.getItem(position).toString()
            selectedStudentId = studentMap[selectedName]

            // Purana data saaf kar dein taake naya load ho sake
            spinnerPickup.text.clear()
            spinnerDropoff.text.clear()
            spinnerPickup.setAdapter(null)
            spinnerDropoff.setAdapter(null)

            cardBusInfo.visibility = View.VISIBLE
            tvBusInfo.text = "Fetching assigned bus from record..."

            // === STEP 2: Record se live fetch karna ke is student ko konsi bus assign hui thi ===
            fetchAssignedBusFromRecord(selectedStudentId!!)
        }

        // === LOGIC: Pickup select hone par Drop-off list filter karna ===
        spinnerPickup.setOnItemClickListener { _, _, position, _ ->
            val selectedPickup = spinnerPickup.adapter.getItem(position).toString()
            updateDropoffList(selectedPickup)
        }

        // === LOGIC: Drop-off select hone par Pickup list filter karna ===
        spinnerDropoff.setOnItemClickListener { _, _, position, _ ->
            val selectedDropoff = spinnerDropoff.adapter.getItem(position).toString()
            updatePickupList(selectedDropoff)
        }

        // === STEP 5: Save button logic ===
        findViewById<MaterialButton>(R.id.btn_save_assignment).setOnClickListener {
            savePointsToDatabase()
        }
    }

    private fun loadOnlyStudents() {
        dbRef.child("students").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val studentNames = ArrayList<String>()
                if (snapshot.exists()) {
                    for (snap in snapshot.children) {
                        val name = snap.child("studentName").value.toString()
                        val studentId = snap.key.toString()

                        studentNames.add(name)
                        studentMap[name] = studentId
                    }
                    val adapter = ArrayAdapter(this@AssignPointsActivity, android.R.layout.simple_dropdown_item_1line, studentNames)
                    spinnerStudent.setAdapter(adapter)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchAssignedBusFromRecord(studentId: String) {
        dbRef.child("students").child(studentId).child("busId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.value.toString() != "null" && snapshot.value.toString().isNotEmpty()) {
                    fetchedBusId = snapshot.value.toString()
                    showBusNameOnCard(fetchedBusId!!)
                    fetchRoutesForBus(fetchedBusId!!)
                } else {
                    fetchedBusId = null
                    tvBusInfo.text = "No Bus Assigned to this Student!"
                    tvBusInfo.setTextColor(ContextCompat.getColor(this@AssignPointsActivity, android.R.color.holo_red_dark))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showBusNameOnCard(busId: String) {
        dbRef.child("buses").child(busId).child("busName").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    tvBusInfo.text = "Assigned Bus: ${snapshot.value.toString()}"
                    tvBusInfo.setTextColor(ContextCompat.getColor(this@AssignPointsActivity, android.R.color.holo_blue_dark))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchRoutesForBus(busId: String) {
        dbRef.child("routes").child(busId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                routeStopsList.clear()
                if (snapshot.exists()) {
                    for (stopSnap in snapshot.children) {
                        val stopKey = stopSnap.key.toString()
                        val formattedName = stopKey.replace("_", " ").split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        }
                        routeStopsList.add(formattedName)
                    }

                    // Shuru mein dono dropdowns mein saari list daal dein
                    val stopsAdapter = ArrayAdapter(this@AssignPointsActivity, android.R.layout.simple_dropdown_item_1line, routeStopsList)
                    spinnerPickup.setAdapter(stopsAdapter)
                    spinnerDropoff.setAdapter(stopsAdapter)

                    Toast.makeText(this@AssignPointsActivity, "Routes Loaded. Please select Pickup and Drop-off.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AssignPointsActivity, "No route defined for this bus!", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // === FUNCTION: Drop-off ki list update karna (Pickup ko nikaal kar) ===
    private fun updateDropoffList(selectedPickup: String) {
        val filteredList = routeStopsList.filter { it != selectedPickup }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filteredList)
        spinnerDropoff.setAdapter(adapter)
    }

    // === FUNCTION: Pickup ki list update karna (Drop-off ko nikaal kar) ===
    private fun updatePickupList(selectedDropoff: String) {
        val filteredList = routeStopsList.filter { it != selectedDropoff }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filteredList)
        spinnerPickup.setAdapter(adapter)
    }

    private fun savePointsToDatabase() {
        val pickup = spinnerPickup.text.toString()
        val dropoff = spinnerDropoff.text.toString()

        if (selectedStudentId == null || fetchedBusId == null || pickup.isEmpty() || dropoff.isEmpty()) {
            Toast.makeText(this, "Please complete all steps before saving.", Toast.LENGTH_SHORT).show()
            return
        }

        if (pickup == dropoff) {
            Toast.makeText(this, "Pickup and Drop-off cannot be the same!", Toast.LENGTH_SHORT).show()
            return
        }

        val dbPickupFormat = pickup.lowercase().replace(" ", "_")
        val dbDropoffFormat = dropoff.lowercase().replace(" ", "_")

        val pointsData = mapOf(
            "pickupStop" to dbPickupFormat,
            "dropoffStop" to dbDropoffFormat
        )

        dbRef.child("students").child(selectedStudentId!!).updateChildren(pointsData)
            .addOnSuccessListener {
                Toast.makeText(this, "Points Assigned Successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}