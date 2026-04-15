package com.example.finalprojectsmartbustrackingsystem

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
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class ManageDriversActivity : AppCompatActivity() {

    private lateinit var rvDrivers: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var driverList: ArrayList<DriverModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_drivers)

        rvDrivers = findViewById(R.id.rv_drivers_list)
        rvDrivers.layoutManager = LinearLayoutManager(this)
        rvDrivers.setHasFixedSize(true)

        driverList = arrayListOf<DriverModel>()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        getDriversData()

        findViewById<MaterialButton>(R.id.btn_add_new_driver).setOnClickListener {
            startActivity(Intent(this, AddDriverActivity::class.java))
        }
    }

    private fun getDriversData() {
        // Sirf un users ko filter karna jinka role 'driver' hai
        dbRef.orderByChild("role").equalTo("driver")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    driverList.clear()
                    if (snapshot.exists()) {
                        for (driverSnap in snapshot.children) {
                            val data = driverSnap.getValue(DriverModel::class.java)
                            driverList.add(data!!)
                        }
                        setupAdapter()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageDriversActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupAdapter() {
        val adapter = object : RecyclerView.Adapter<DriverViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_driver, parent, false)
                return DriverViewHolder(view)
            }

            override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
                val current = driverList[position]
                holder.name.text = current.name
                holder.id.text = "ID: ${current.driverId}"

                // --- 1. DELETE LOGIC ---
                holder.btnDelete.setOnClickListener {
                    val builder = AlertDialog.Builder(this@ManageDriversActivity)
                    builder.setTitle("Delete Driver")
                    builder.setMessage("Do you want to delete ${current.name} from the list?")
                    builder.setPositiveButton("Yes, Delete") { _, _ ->
                        // Firebase se record delete karna
                        current.uid?.let { uid ->
                            dbRef.child(uid).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this@ManageDriversActivity, "Driver Deleted", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    builder.setNegativeButton("Cancel", null)
                    builder.show()
                }

                // --- 2. EDIT LOGIC ---
                holder.btnEdit.setOnClickListener {
                    // Wapis form par bhejna data ke sath
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
        rvDrivers.adapter = adapter
    }


    class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_display_name)
        val id: TextView = itemView.findViewById(R.id.tv_display_id)
        val btnEdit: View = itemView.findViewById(R.id.iv_edit_driver)
        val btnDelete: View = itemView.findViewById(R.id.iv_delete_driver)
    }
}