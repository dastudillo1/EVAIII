package com.example.evaiii

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.evaiii.ui.theme.EvaIIITheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapaCompletoActivity : AppCompatActivity() {

    private var latitud: Double = 0.0
    private var longitud: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_completo)

        // Obtener datos de latitud y longitud del intent
        latitud = intent.getDoubleExtra("LATITUD", 0.0)
        longitud = intent.getDoubleExtra("LONGITUD", 0.0)

        // Configurar y mostrar el mapa en pantalla completa
        mostrarMapaCompleto()
    }

    private fun mostrarMapaCompleto() {
        val mapView = findViewById<MapView>(R.id.mapView) // Cambiar por el ID de tu MapView en el layout

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        Configuration.getInstance().userAgentValue = packageName

        val mapController = mapView.controller
        val startPoint = GeoPoint(latitud, longitud)
        mapController.setZoom(18.0)
        mapController.setCenter(startPoint)

        val marker = Marker(mapView)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        mapView.overlays.add(marker)
    }
}

