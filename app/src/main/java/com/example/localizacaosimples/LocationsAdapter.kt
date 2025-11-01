package com.example.localizacaosimples

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class LocationsAdapter(
    private val locations: List<UserLocation>
) : RecyclerView.Adapter<LocationsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userNameTextView: TextView = view.findViewById(R.id.userNameTextView)
        val youBadgeTextView: TextView = view.findViewById(R.id.youBadgeTextView)
        val coordinatesTextView: TextView = view.findViewById(R.id.coordinatesTextView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val openMapsButton: Button = view.findViewById(R.id.openMapsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_localizacao, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = locations[position]

        holder.userNameTextView.text = location.userName

        // Mostrar badge "Você" se for o usuário atual
        if (location.isCurrentUser) {
            holder.youBadgeTextView.visibility = View.VISIBLE
        } else {
            holder.youBadgeTextView.visibility = View.GONE
        }

        // Formatar coordenadas
        holder.coordinatesTextView.text =
            "📍 Lat: ${"%.6f".format(location.latitude)}, Lon: ${"%.6f".format(location.longitude)}"

        // Calcular tempo desde última atualização
        val timeAgo = getTimeAgo(location.timestamp)
        holder.timestampTextView.text = "⏱️ Atualizado $timeAgo"

        // Abrir no Google Maps
        holder.openMapsButton.setOnClickListener {
            val context = holder.itemView.context
            val uri = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${location.userName})"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Se o Google Maps não estiver instalado, abrir no navegador
                val browserUri = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(browserUri))
                context.startActivity(browserIntent)
            }
        }
    }

    override fun getItemCount() = locations.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "há ${TimeUnit.MILLISECONDS.toSeconds(diff)} segundos"
            diff < 3600000 -> "há ${TimeUnit.MILLISECONDS.toMinutes(diff)} minutos"
            diff < 86400000 -> "há ${TimeUnit.MILLISECONDS.toHours(diff)} horas"
            else -> "há ${TimeUnit.MILLISECONDS.toDays(diff)} dias"
        }
    }
}