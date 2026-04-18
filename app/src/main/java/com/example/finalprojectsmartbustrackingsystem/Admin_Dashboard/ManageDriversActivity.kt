package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalprojectsmartbustrackingsystem.DriverModel
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import android.app.TimePickerDialog
import java.util.Calendar

class ManageDriversActivity : AppCompatActivity() {

    private lateinit var rvDrivers: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var driverList: ArrayList<DriverModel>

    private lateinit var driverAdapter: RecyclerView.Adapter<DriverViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_drivers)

        rvDrivers = findViewById(R.id.rv_drivers_list)
        rvDrivers.layoutManager = LinearLayoutManager(this)
        rvDrivers.setHasFixedSize(true)

        driverList = arrayListOf<DriverModel>()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        initAdapter()
        getDriversData()

        findViewById<MaterialButton>(R.id.btn_add_new_driver).setOnClickListener {
            startActivity(Intent(this, AddDriverActivity::class.java))
        }
    }

    private fun initAdapter() {
        driverAdapter = object : RecyclerView.Adapter<DriverViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_driver, parent, false)
                return DriverViewHolder(view)
            }

            override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
                val current = driverList[position]
                holder.name.text = current.name
                holder.id.text = "ID: ${current.driverId}"

                val busName = current.assignedBusName ?: "Not Assigned"

                // === NAYA HISSA: Shift Timing Show Karna ===
                val shiftStart = current.shiftStart ?: "--:--"
                val shiftEnd = current.shiftEnd ?: "--:--"
                holder.busInfo.text = "Bus: $busName\nShift: $shiftStart to $shiftEnd"

                // --- 1. SHIFT UPDATE LOGIC (Naya) ---
                holder.btnShift.setOnClickListener {
                    showShiftUpdateDialog(current)
                }

                // --- 2. DELETE LOGIC ---
                // ManageDriversActivity.kt mein delete logic ko update karein
                holder.btnDelete.setOnClickListener {
                    val builder = AlertDialog.Builder(this@ManageDriversActivity)
                    builder.setTitle("Delete Driver")
                    builder.setMessage("Do you want to delete this driver?")

                    builder.setPositiveButton("Yes, Delete") { dialog, _ ->
                        val driverUid = current.uid
                        val busId = current.assignedBusId

                        if (driverUid != null) {
                            val rootRef = FirebaseDatabase.getInstance().reference
                            val updates = HashMap<String, Any?>()

                            // 1. Driver ko users list se nikalna
                            updates["users/$driverUid"] = null

                            // 2. Agar koi bus assign thi, to usay free karna
                            if (!busId.isNullOrEmpty() && busId != "Not Assigned") {
                                updates["buses/$busId/isAssigned"] = false
                                updates["buses/$busId/assignedDriverId"] = "Not Assigned"
                                updates["buses/$busId/assignedDriverName"] = "Not Assigned"
                            }

                            // 3. Students ka data update karne ke liye query
                            rootRef.child("students").orderByChild("driverId").equalTo(driverUid)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (studentSnap in snapshot.children) {
                                            val studentId = studentSnap.key
                                            if (studentId != null) {
                                                // Student ke record se driver info hatana
                                                updates["students/$studentId/driverId"] = "Not Assigned"
                                                updates["students/$studentId/driverName"] = "Not Assigned"
                                            }
                                        }

                                        // Atomic Update: Sab kaam aik sath database mein honge
                                        rootRef.updateChildren(updates).addOnSuccessListener {
                                            Toast.makeText(this@ManageDriversActivity, "Driver and linked data removed successfully", Toast.LENGTH_SHORT).show()
                                        }.addOnFailureListener {
                                            Toast.makeText(this@ManageDriversActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                        dialog.dismiss()
                    }
                    builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    builder.show()
                }

                // --- 3. EDIT LOGIC ---
                holder.btnEdit.setOnClickListener {
                    val intent = Intent(this@ManageDriversActivity, AddDriverActivity::class.java)
                    intent.putExtra("action", "edit")
                    intent.putExtra("uid", current.uid)
                    intent.putExtra("name", current.name)
                    intent.putExtra("email", current.email)
                    intent.putExtra("phone", current.phone)
                    startActivity(intent)
                }
            }

            override fun getItemCount(): Int = driverList.size
        }
        rvDrivers.adapter = driverAdapter
    }

    // --- NAYA FUNCTION: Dialog Box Open Karna ---
    private fun showShiftUpdateDialog(driver: DriverModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_shift, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvDriverName = dialogView.findViewById<TextView>(R.id.tv_driver_name_dialog)
        val etStart = dialogView.findViewById<TextInputEditText>(R.id.et_shift_start)
        val etEnd = dialogView.findViewById<TextInputEditText>(R.id.et_shift_end)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save_shift)

        tvDriverName.text = "Driver: ${driver.name}"
        etStart.setText(driver.shiftStart)
        etEnd.setText(driver.shiftEnd)

        // --- NAYA HISSA: Time Picker Logic ---

        // Start Time Click Listener
        etStart.setOnClickListener {
            showTimePicker(etStart)
        }

        // End Time Click Listener
        etEnd.setOnClickListener {
            showTimePicker(etEnd)
        }

        btnSave.setOnClickListener {
            val startStr = etStart.text.toString().trim()
            val endStr = etEnd.text.toString().trim()

            if (startStr.isEmpty() || endStr.isEmpty()) {
                Toast.makeText(this, "Please select both timings", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase update logic wahi rahegi
            driver.uid?.let { uid ->
                val updates = mapOf(
                    "shiftStart" to startStr,
                    "shiftEnd" to endStr
                )
                dbRef.child(uid).updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "Shift Updated Successfully!", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                }
            }
        }
        alertDialog.show()
    }

    // Helper Function: Time Picker show karne aur format karne ke liye
    private fun showTimePicker(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->

            // 12-Hour format (AM/PM) mein convert karna
            val isPM = selectedHour >= 12
            val formattedHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
            val amPm = if (isPM) "PM" else "AM"

            // Time ko 07:30 AM format mein string banana
            val timeString = String.format("%02d:%02d %s", formattedHour, selectedMinute, amPm)
            editText.setText(timeString)

        }, currentHour, currentMinute, false) // false ka matlab hai 12-hour clock dikhao

        timePickerDialog.show()
    }

    private fun getDriversData() {
        dbRef.orderByChild("role").equalTo("driver")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    driverList.clear()
                    if (snapshot.exists()) {
                        for (driverSnap in snapshot.children) {
                            val data = driverSnap.getValue(DriverModel::class.java)
                            data?.let { driverList.add(it) }
                        }
                    }
                    driverAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_display_name)
        val id: TextView = itemView.findViewById(R.id.tv_display_id)
        val busInfo: TextView = itemView.findViewById(R.id.tv_assigned_bus_display)

        // Buttons
        val btnShift: View = itemView.findViewById(R.id.iv_shift_driver) // NAYA VIEW LINK KIYA
        val btnEdit: View = itemView.findViewById(R.id.iv_edit_driver)
        val btnDelete: View = itemView.findViewById(R.id.iv_delete_driver)
    }
}