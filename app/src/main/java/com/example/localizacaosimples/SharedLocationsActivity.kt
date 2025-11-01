package com.example.localizacaosimples

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

data class UserLocation(
    val userId: String = "",
    val userName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0,
    val isCurrentUser: Boolean = false
)

class SharedLocationsActivity : AppCompatActivity() {

    private var locationsRecyclerView: RecyclerView? = null
    private var emptyTextView: TextView? = null

    private val database = FirebaseDatabase.getInstance()
    private val locationsRef = database.getReference("localizacao_atual")
    private val sharingRef = database.getReference("compartilhamentos")
    private val usersRef = database.getReference("usuarios")

    private var currentUserId: String? = null
    private var currentUserName: String? = null
    private val locations = mutableListOf<UserLocation>()
    private var adapter: LocationsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shared_locations)

        supportActionBar?.title = "📍 Localizações"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Carregar dados do usuário
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getString("userId", null)
        currentUserName = sharedPref.getString("nome", "Você")

        if (currentUserId == null) {
            Toast.makeText(this, "Erro: Usuário não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar views
        try {
            locationsRecyclerView = findViewById<RecyclerView>(R.id.locationsRecyclerView)
            emptyTextView = findViewById<TextView>(R.id.emptyTextView)

            // Configurar RecyclerView
            adapter = LocationsAdapter(locations)
            locationsRecyclerView?.layoutManager = LinearLayoutManager(this)
            locationsRecyclerView?.adapter = adapter

            loadLocations()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao inicializar: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun loadLocations() {
        locations.clear()

        // Adicionar localização do usuário atual
        loadUserLocation(currentUserId!!, currentUserName!!, true)

        // Buscar usuários compartilhados
        sharingRef.child(currentUserId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    updateUI()
                    return
                }

                val sharedUserIds = snapshot.children.mapNotNull { it.key }

                if (sharedUserIds.isEmpty()) {
                    updateUI()
                    return
                }

                // Carregar nome e localização de cada usuário compartilhado
                sharedUserIds.forEach { userId ->
                    usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val userName = userSnapshot.child("nome").getValue(String::class.java) ?: "Usuário"
                            loadUserLocation(userId, userName, false)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Ignorar erro
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@SharedLocationsActivity,
                    "Erro ao carregar compartilhamentos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadUserLocation(userId: String, userName: String, isCurrentUser: Boolean) {
        locationsRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                    // Remover localização anterior deste usuário (se existir)
                    locations.removeAll { it.userId == userId }

                    // Adicionar nova localização
                    locations.add(UserLocation(
                        userId = userId,
                        userName = userName,
                        latitude = latitude,
                        longitude = longitude,
                        timestamp = timestamp,
                        isCurrentUser = isCurrentUser
                    ))

                    // Ordenar: usuário atual primeiro
                    locations.sortByDescending { it.isCurrentUser }

                    updateUI()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignorar erro
            }
        })
    }

    private fun updateUI() {
        if (locations.isEmpty()) {
            locationsRecyclerView?.visibility = View.GONE
            emptyTextView?.visibility = View.VISIBLE
        } else {
            locationsRecyclerView?.visibility = View.VISIBLE
            emptyTextView?.visibility = View.GONE
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}