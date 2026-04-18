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
import com.example.finalprojectsmartbustrackingsystem.BusModel
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class BusListActivity : AppCompatActivity() {

    private lateinit var rvBuses: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var busList: ArrayList<BusModel>

    // Naya Variable: Adapter ko class level par define kiya
    private lateinit var busAdapter: RecyclerView.Adapter<BusViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_list)

        // Initialize RecyclerView
        rvBuses = findViewById(R.id.rv_buses_list)
        rvBuses.layoutManager = LinearLayoutManager(this)
        rvBuses.setHasFixedSize(true)

        busList = arrayListOf<BusModel>()
        dbRef = FirebaseDatabase.getInstance().getReference("buses")

        // 1. Adapter ko sirf ek dafa initialize karna
        initAdapter()

        // 2. Fetch Data from Firebase
        getBusesData()

        // Add New Bus Button Click
        findViewById<MaterialButton>(R.id.btn_add_new_bus_from_list).setOnClickListener {
            val intent = Intent(this, AddBusActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initAdapter() {
        busAdapter = object : RecyclerView.Adapter<BusViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus, parent, false)
                return BusViewHolder(view)
            }

            override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
                val current = busList[position]
                holder.name.text = current.busName
                holder.plate.text = "Plate: ${current.licensePlate}"
                holder.capacity.text = "Seats: ${current.capacity}"

                // --- DELETE LOGIC ---
                // BusListActivity.kt mein delete button ki logic ko update karein
                holder.btnDelete.setOnClickListener {
                    val builder = AlertDialog.Builder(this@BusListActivity)
                    builder.setTitle("Delete Bus")
                    builder.setMessage("Do you want to delete this bus?")

                    builder.setPositiveButton("Yes, Delete") { dialog, _ ->
                        val busId = current.busId
                        val driverUid = current.assignedDriverId// Yeh BusModel mein hona chahiye

                        if (busId != null) {
                            val rootRef = FirebaseDatabase.getInstance().reference
                            val updates = HashMap<String, Any?>()

                            // 1. Bus ko database se delete karna
                            updates["buses/$busId"] = null

                            // 2. Driver ko free karna (Agar koi driver assign tha)
                            if (!driverUid.isNullOrEmpty() && driverUid != "Not Assigned") {
                                updates["users/$driverUid/isAvailable"] = true
                                updates["users/$driverUid/assignedBusId"] = null
                                updates["users/$driverUid/assignedBusName"] = "Not Assigned"
                            }

                            // 3. Students ka data update karna jo is bus mein thay
                            rootRef.child("students").orderByChild("busId").equalTo(busId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (studentSnap in snapshot.children) {
                                            val studentId = studentSnap.key
                                            if (studentId != null) {
                                                // Student ke record se bus aur driver dono ki info hatana
                                                updates["students/$studentId/busId"] = "Not Assigned"
                                                updates["students/$studentId/busName"] = "Not Assigned"
                                                updates["students/$studentId/driverId"] = "Not Assigned"
                                                updates["students/$studentId/driverName"] = "Not Assigned"
                                            }
                                        }

                                        // Atomic Update: Saara cleaning process aik hi dafa mein
                                        rootRef.updateChildren(updates).addOnSuccessListener {
                                            Toast.makeText(this@BusListActivity, "Bus and linked records updated successfully", Toast.LENGTH_SHORT).show()
                                        }.addOnFailureListener {
                                            Toast.makeText(this@BusListActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
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

                // --- EDIT LOGIC ---
                holder.btnEdit.setOnClickListener {
                    val intent = Intent(this@BusListActivity, AddBusActivity::class.java)
                    intent.putExtra("action", "edit")
                    intent.putExtra("busId", current.busId)
                    intent.putExtra("busName", current.busName)
                    intent.putExtra("licensePlate", current.licensePlate)
                    intent.putExtra("capacity", current.capacity)
                    startActivity(intent)
                }
            }

            override fun getItemCount(): Int = busList.size
        }

        // RecyclerView ko adapter assign karna
        rvBuses.adapter = busAdapter
    }

    private fun getBusesData() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                busList.clear()
                if (snapshot.exists()) {
                    for (busSnap in snapshot.children) {
                        val data = busSnap.getValue(BusModel::class.java)
                        data?.let { busList.add(it) }
                    }
                }
                // MAIN FIX: Adapter ko data change hone ka signal dena
                busAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BusListActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ViewHolder class to link item_bus.xml views
    class BusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_bus_display_name)
        val plate: TextView = itemView.findViewById(R.id.tv_bus_display_plate)
        val capacity: TextView = itemView.findViewById(R.id.tv_bus_display_capacity)
        val btnEdit: ImageView = itemView.findViewById(R.id.iv_edit_bus)
        val btnDelete: ImageView = itemView.findViewById(R.id.iv_delete_bus)
    }
}