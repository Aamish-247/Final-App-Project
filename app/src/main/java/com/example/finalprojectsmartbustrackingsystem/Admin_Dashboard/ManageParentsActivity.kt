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
import com.example.finalprojectsmartbustrackingsystem.ParentModel
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class ManageParentsActivity : AppCompatActivity() {

    private lateinit var rvParents: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var parentList: ArrayList<ParentModel>
    private lateinit var parentAdapter: RecyclerView.Adapter<ParentViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_parents)

        rvParents = findViewById(R.id.rv_parents_list)
        rvParents.layoutManager = LinearLayoutManager(this)
        rvParents.setHasFixedSize(true)

        parentList = arrayListOf<ParentModel>()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        initAdapter()
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

                holder.btnDelete.setOnClickListener {
                    val builder = AlertDialog.Builder(this@ManageParentsActivity)
                    builder.setTitle("Delete Parent")
                    builder.setMessage("Are you sure you want to delete ${current.name} and all linked students?")
                    builder.setPositiveButton("Yes, Delete All") { dialog, _ ->
                        current.uid?.let { uid ->
                            deleteParentWithCascade(uid)
                        }
                        dialog.dismiss()
                    }
                    builder.setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                }

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
        rvParents.adapter = parentAdapter
    }

    private fun getParentsData() {
        dbRef.orderByChild("role").equalTo("parent")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    parentList.clear()
                    if (snapshot.exists()) {
                        for (parentSnap in snapshot.children) {
                            val data = parentSnap.getValue(ParentModel::class.java)
                            data?.let { parentList.add(it) }
                        }
                    }
                    parentAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageParentsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteParentWithCascade(parentId: String) {
        val rootRef = FirebaseDatabase.getInstance().reference

        rootRef.child("students").orderByChild("parentId").equalTo(parentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updates = HashMap<String, Any?>()

                    if (snapshot.exists()) {
                        for (studentSnap in snapshot.children) {
                            updates["students/${studentSnap.key}"] = null
                        }
                    }

                    // 2. Parent ko users node se delete karo
                    updates["users/$parentId"] = null

                    // 3. Ek hi atomic operation mein sab delete karein
                    rootRef.updateChildren(updates).addOnSuccessListener {
                        Toast.makeText(this@ManageParentsActivity, "Parent and linked students removed successfully", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this@ManageParentsActivity, "Delete failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_parent_display_name)
        val id: TextView = itemView.findViewById(R.id.tv_parent_display_id)
        val btnEdit: View = itemView.findViewById(R.id.iv_edit_parent)
        val btnDelete: View = itemView.findViewById(R.id.iv_delete_parent)
    }
}