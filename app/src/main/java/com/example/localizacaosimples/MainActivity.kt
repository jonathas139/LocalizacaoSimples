package com.example.localizacaosimples

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView
    private val database = FirebaseDatabase.getInstance()
    private val historicRef = database.getReference("localizacoes")
    private val currentLocationRef = database.getReference("localizacao_atual")

    private var userId: String? = null
    private var userName: String? = null

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 10000L
    ).apply {
        setMinUpdateIntervalMillis(5000L)
        setMaxUpdateDelayMillis(15000L)
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                updateLocationUI(location)
                sendLocationToFirebase(location)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                checkLocationEnabledAndStart()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                checkLocationEnabledAndStart()
            }
            else -> {
                locationTextView.text = "❌ Permissão de localização negada."
                Toast.makeText(this, "Permissão necessária para funcionamento", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationTextView = findViewById(R.id.locationTextView)

        loadUserData()
        supportActionBar?.title = "$userName"

        checkPermissionAndStart()
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userId = sharedPref.getString("userId", null)
        userName = sharedPref.getString("nome", "Usuário")

        if (userId == null) {
            Toast.makeText(this, "Faça login novamente", Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_locations -> {
                startActivity(Intent(this, SharedLocationsActivity::class.java))
                true
            }
            R.id.action_share -> {
                startActivity(Intent(this, ManageSharingActivity::class.java))
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        fusedLocationClient.removeLocationUpdates(locationCallback)

        Toast.makeText(this, "Logout realizado", Toast.LENGTH_SHORT).show()
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkPermissionAndStart() {
        when {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkLocationEnabledAndStart()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun checkLocationEnabledAndStart() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            locationTextView.text = "⚠️ GPS desativado.\nPor favor, ative a localização nas configurações."
            Toast.makeText(this, "Ative o GPS para continuar", Toast.LENGTH_LONG).show()
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationTextView.text = "🔄 Aguardando localização..."

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        ).addOnSuccessListener {
            Toast.makeText(this, "Rastreamento iniciado", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            locationTextView.text = "❌ Erro ao iniciar rastreamento: ${e.message}"
        }
    }

    private fun updateLocationUI(location: Location) {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(Date())

        locationTextView.text = """
            
          
            📍 Localização Atual
            
            Latitude: ${"%.6f".format(location.latitude)}
            Longitude: ${"%.6f".format(location.longitude)}
            Precisão: ${"%.1f".format(location.accuracy)}m
            
            ⏱️ Atualizado: $currentTime
            
            ✅ Enviando para seu histórico...
        """.trimIndent()
    }

    private fun sendLocationToFirebase(location: Location) {
        val timestamp = System.currentTimeMillis()
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val dados = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to timestamp,
            "dateTime" to dateTime,
            "userName" to userName
        )

        // Salvar no histórico
        historicRef.child(userId!!).push().setValue(dados)

        // Atualizar localização atual (sobrescreve a anterior)
        currentLocationRef.child(userId!!).setValue(dados)
            .addOnFailureListener { e ->
                runOnUiThread {
                    locationTextView.append("\n\n❌ Erro ao enviar: ${e.message}")
                }
                Toast.makeText(this, "Falha ao enviar dados", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }
}