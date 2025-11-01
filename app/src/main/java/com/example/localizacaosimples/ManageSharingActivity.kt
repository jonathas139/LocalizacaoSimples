package com.example.localizacaosimples

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

data class SharedUser(
    val userId: String = "",
    val nome: String = "",
    val email: String = ""
)

class ManageSharingActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var shareButton: Button
    private lateinit var sharingRecyclerView: RecyclerView
    private lateinit var emptyTextView: TextView

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("usuarios")
    private val sharingRef = database.getReference("compartilhamentos")

    private var currentUserId: String? = null
    private val sharedUsers = mutableListOf<SharedUser>()
    private lateinit var adapter: SharingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_sharing)

        supportActionBar?.title = "🔗 Gerenciar Compartilhamento"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Carregar userId
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getString("userId", null)

        if (currentUserId == null) {
            Toast.makeText(this, "Erro: Usuário não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        emailEditText = findViewById(R.id.emailEditText)
        shareButton = findViewById(R.id.shareButton)
        sharingRecyclerView = findViewById(R.id.sharingRecyclerView)
        emptyTextView = findViewById(R.id.emptyTextView)

        // Configurar RecyclerView
        adapter = SharingAdapter(sharedUsers) { user ->
            showRemoveDialog(user)
        }
        sharingRecyclerView.layoutManager = LinearLayoutManager(this)
        sharingRecyclerView.adapter = adapter

        shareButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                shareWithUser(email)
            } else {
                Toast.makeText(this, "Digite um email", Toast.LENGTH_SHORT).show()
            }
        }

        loadSharedUsers()
    }

    private fun shareWithUser(email: String) {
        // Buscar usuário pelo email
        usersRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val targetUserId = userSnapshot.key ?: continue

                            if (targetUserId == currentUserId) {
                                Toast.makeText(
                                    this@ManageSharingActivity,
                                    "Você não pode compartilhar consigo mesmo!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }

                            // Adicionar compartilhamento
                            sharingRef.child(currentUserId!!).child(targetUserId).setValue(true)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@ManageSharingActivity,
                                        "✅ Compartilhamento ativado!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    emailEditText.text.clear()
                                    loadSharedUsers()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this@ManageSharingActivity,
                                        "Erro: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } else {
                        Toast.makeText(
                            this@ManageSharingActivity,
                            "❌ Usuário não encontrado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ManageSharingActivity,
                        "Erro: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadSharedUsers() {
        sharingRef.child(currentUserId!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                sharedUsers.clear()

                if (!snapshot.exists()) {
                    showEmptyState()
                    return
                }

                val userIds = snapshot.children.mapNotNull { it.key }

                if (userIds.isEmpty()) {
                    showEmptyState()
                    return
                }

                // Buscar dados de cada usuário
                var loadedCount = 0
                userIds.forEach { userId ->
                    usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val nome = userSnapshot.child("nome").getValue(String::class.java) ?: "Sem nome"
                            val email = userSnapshot.child("email").getValue(String::class.java) ?: ""

                            sharedUsers.add(SharedUser(userId, nome, email))
                            loadedCount++

                            if (loadedCount == userIds.size) {
                                updateUI()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            loadedCount++
                            if (loadedCount == userIds.size) {
                                updateUI()
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ManageSharingActivity,
                    "Erro ao carregar: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showEmptyState() {
        sharingRecyclerView.visibility = View.GONE
        emptyTextView.visibility = View.VISIBLE
    }

    private fun updateUI() {
        if (sharedUsers.isEmpty()) {
            showEmptyState()
        } else {
            sharingRecyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE
            adapter.notifyDataSetChanged()
        }
    }

    private fun showRemoveDialog(user: SharedUser) {
        AlertDialog.Builder(this)
            .setTitle("Remover Compartilhamento")
            .setMessage("Deseja parar de compartilhar sua localização com ${user.nome}?")
            .setPositiveButton("Sim") { _, _ ->
                removeSharing(user)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun removeSharing(user: SharedUser) {
        sharingRef.child(currentUserId!!).child(user.userId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Compartilhamento removido", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}