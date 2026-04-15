package com.example.finalprojectsmartbustrackingsystem

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
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class ManageParentsActivity : AppCompatActivity() {

    private lateinit var rvParents: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var parentList: ArrayList<ParentModel>

    // Naya Variable: Adapter ko class level par define kiya hai
    private lateinit var parentAdapter: RecyclerView.Adapter<ParentViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_parents)

        rvParents = findViewById(R.id.rv_parents_list)
        rvParents.layoutManager = LinearLayoutManager(this)
        rvParents.setHasFixedSize(true)

        parentList = arrayListOf<ParentModel>()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        // 1. Adapter ko sirf ek dafa initialize karna
        initAdapter()

        // 2. Data mangwana
        getParentsData()

        findViewById<MaterialButton>(R.id.btn_add_new_parent).setOnClickListener {
            startActivity(Intent(this, AddParentActivity::class.java))
        }
    }

    private fun initAdapter() {
        parentAdapter = object : RecyclerView.Adapter<ParentViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_parent, parent, false)
                return ParentViewHolder(view)
            }

            override fun onBindViewHolder(holder: ParentViewHolder, position: Int) {
                val current = parentList[position]
                holder.name.text = current.name
                holder.id.text = "ID: ${current.parentId}"

                // --- DELETE LOGIC ---
                holder.btnDelete.setOnClickListener {
                    val builder = AlertDialog.Builder(this@ManageParentsActivity)
                    builder.setTitle("Delete Parent")
                    builder.setMessage("Do you want to delete ${current.name} from the list?")
                    builder.setPositiveButton("Yes, Delete") { dialog, _ ->
                        current.uid?.let { uid ->
                            dbRef.child(uid).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this@ManageParentsActivity, "Parent Deleted Successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@ManageParentsActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        dialog.dismiss()
                    }
                    builder.setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                }

                // --- EDIT LOGIC ---
                holder.btnEdit.setOnClickListener {
                    val intent = Intent(this@ManageParentsActivity, AddParentActivity::class.java)
                    intent.putExtra("action", "edit")
                    intent.putExtra("uid", current.uid)
                    intent.putExtra("name", current.name)
                    intent.putExtra("email", current.email)
                    intent.putExtra("phone", current.phone)
                    startActivity(intent)
                }
            }

            override fun getItemCount(): Int = parentList.size
        }

        // RecyclerView ko adapter assign kar diya
        rvParents.adapter = parentAdapter
    }

    private fun getParentsData() {
        // Sirf un users ko filter karna jinka role 'parent' hai
        dbRef.orderByChild("role").equalTo("parent")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    parentList.clear() // Purani list saaf ki
                    if (snapshot.exists()) {
                        for (parentSnap in snapshot.children) {
                            val data = parentSnap.getValue(ParentModel::class.java)
                            data?.let { parentList.add(it) }
                        }
                    }
                    // MAIN FIX: Adapter ko batana ke data update ho gaya hai taake wo fauran UI refresh kare
                    parentAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageParentsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_parent_display_name)
        val id: TextView = itemView.findViewById(R.id.tv_parent_display_id)
        val btnEdit: View = itemView.findViewById(R.id.iv_edit_parent)
        val btnDelete: View = itemView.findViewById(R.id.iv_delete_parent)
    }
}