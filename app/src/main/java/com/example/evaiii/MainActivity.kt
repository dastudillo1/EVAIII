package com.example.evaiii

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evaiii.ui.theme.EvaIIITheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import coil.compose.rememberImagePainter
//import dev.chrisbanes.accompanist.coil.rememberCoilPainter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException

// pantallas disponibles
enum class Pantalla {
    FORM,
    FOTO
}
// ViewModel para la app de la cámara
class CameraAppViewModel : ViewModel() {
    val pantalla = mutableStateOf(Pantalla.FORM)
    // callbacks
    var onPermisoCamaraOk : () -> Unit = {}
    var onPermisoUbicacionOk: () -> Unit = {}
    // lanzador permisos
    var lanzadorPermisos: ActivityResultLauncher<Array<String>>? = null
    fun cambiarPantallaFoto(){ pantalla.value = Pantalla.FOTO }
    fun cambiarPantallaForm(){ pantalla.value = Pantalla.FORM }
}
// ViewModel para el formulario de recepción
class FormRecepcionViewModel : ViewModel() {
    val receptor = mutableStateOf("")
    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)
    val fotoRecepcion = mutableStateOf<Uri?>(null)
    val fotoRecepcionVarias = mutableListOf<Uri>()
    // Nueva propiedad para la foto seleccionada en pantalla completa
    val fotoSeleccionada = mutableStateOf<Uri?>(null)
    val fotoRecepcionList = mutableStateListOf<Uri>()
}
// Clase principal del activity
class MainActivity : ComponentActivity() {
    val cameraAppVm:CameraAppViewModel by viewModels()
    lateinit var cameraController: LifecycleCameraController
    val lanzadorPermisos =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {
            when {
                (it[android.Manifest.permission.ACCESS_FINE_LOCATION] ?:
                false) or (it[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?:
                false) -> {
                    Log.v("callback RequestMultiplePermissions", "permiso ubicacion otorgado")
                            cameraAppVm.onPermisoUbicacionOk()
                }
                (it[android.Manifest.permission.CAMERA] ?: false) -> {
                    Log.v("callback RequestMultiplePermissions", "permiso camara otorgado")
                            cameraAppVm.onPermisoCamaraOk()
                }
                else -> {
                }
            }
        }
    private fun setupCamara() {
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector =
            CameraSelector.DEFAULT_BACK_CAMERA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraAppVm.lanzadorPermisos = lanzadorPermisos
        setupCamara()
        setContent {
            AppUI(cameraController)
        }
    }
}
// Función para generar un nombre de archivo basado en la fecha y hora actual
fun generarNombreSegunFechaHastaSegundo():String = LocalDateTime
    .now().toString().replace(Regex("[T:.-]"), "").substring(0, 14)

// Función para guardar una foto en la galería del dispositivo
fun guardarFotoEnGaleria(context: Context, archivoFoto: File, nombre: String, descripcion: String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, nombre)
        put(MediaStore.Images.Media.DESCRIPTION, descripcion)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val contentResolver = context.contentResolver
    val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val inputStream = FileInputStream(archivoFoto)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        } catch (e: IOException) {
            // Manejar el error en caso de que ocurra una excepción al guardar la imagen
            e.printStackTrace()
        }
    }
}

// Función para convertir una URI a un bitmap de imagen
fun uri2imageBitmap(uri:Uri, contexto:Context) =
    BitmapFactory.decodeStream(
        contexto.contentResolver.openInputStream(uri)
    ).asImageBitmap()

fun tomarFotografia(
    cameraController: CameraController,
    archivo: File,
    contexto: Context,
    formRecepcionVm:FormRecepcionViewModel,
    imagenGuardadaOk: (uri: Uri) -> Unit
) {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(archivo).build()
    cameraController.takePicture(outputFileOptions,
        ContextCompat.getMainExecutor(contexto), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.also {
                    Log.v("tomarFotografia()::onImageSaved", "Foto guardada en ${it.toString()}")
                    imagenGuardadaOk(it)
                    guardarFotoEnGaleria(contexto, archivo, formRecepcionVm.receptor.value, "DescripciónImagen")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("tomarFotografia()", "Error: ${exception.message}")
            }
        })
}
// Excepción personalizada para manejar falta de permisos
class SinPermisoException(mensaje:String) : Exception(mensaje)

// Función para obtener la ubicación actual del dispositivo
fun getUbicacion(contexto: Context, onUbicacionOk:(location: Location) ->
Unit):Unit {
    try {
        val servicio =
            LocationServices.getFusedLocationProviderClient(contexto)
        val tarea =
            servicio.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        tarea.addOnSuccessListener {
            onUbicacionOk(it)
        }
    } catch (e:SecurityException) {
        throw SinPermisoException(e.message?:"No tiene permisos para conseguir la ubicación")
    }
}

@Composable
fun AppUI(cameraController: CameraController) {
    // Interfaz de usuario para la app basada en el estado del ViewModel
    val contexto = LocalContext.current
    val formRecepcionVm:FormRecepcionViewModel = viewModel()
    val cameraAppViewModel:CameraAppViewModel = viewModel()
    when(cameraAppViewModel.pantalla.value) {
        Pantalla.FORM -> {
            PantallaFormUI(
                formRecepcionVm,
                tomarFotoOnClick = {
                    cameraAppViewModel.cambiarPantallaFoto()
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.CAMERA))
                    //cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(Manifest.permission.CAMERA))
                },
                actualizarUbicacionOnClick = {
                    cameraAppViewModel.onPermisoUbicacionOk = {
                        getUbicacion(contexto) {
                            formRecepcionVm.latitud.value = it.latitude
                            formRecepcionVm.longitud.value = it.longitude
                        }
                    }
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            )
        }
        Pantalla.FOTO -> {
            PantallaFotoUI(formRecepcionVm, cameraAppViewModel,
                cameraController)
        }
        else -> {
            Log.v("AppUI()", "when else, no debería entrar aquí")
        }
    }
}

// Composable UI para la pantalla del formulario
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormUI(
    formRecepcionVm:FormRecepcionViewModel,
    tomarFotoOnClick:() -> Unit = {},
    actualizarUbicacionOnClick:() -> Unit = {}
) {
    val contexto = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            label = { Text("Nombre lugar visitado") },
            value = formRecepcionVm.receptor.value,
            onValueChange = {formRecepcionVm.receptor.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
        Text("Galeria fotografica:")
        Button(onClick = {
            tomarFotoOnClick()
        }) {
            Text("Tomar Fotografía")
        }
        // Mostrar todas las fotos tomadas
        formRecepcionVm.fotoRecepcionVarias.forEach { fotoUri ->
            Box(
                modifier = Modifier
                    .size(200.dp, 100.dp)
                    .clickable {
                        // Al hacer clic en una foto, se establece como foto seleccionada
                        formRecepcionVm.fotoSeleccionada.value = fotoUri
                    }
            ) {
                Image(
                    painter = BitmapPainter(uri2imageBitmap(fotoUri, contexto)),
                    contentDescription = "Imagen lugar visitado${formRecepcionVm.receptor.value}"
                )
            }
        }

        // Mostrar foto seleccionada en pantalla completa
        formRecepcionVm.fotoSeleccionada.value?.let { fotoUri ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Centrar la imagen en la pantalla completa
                Image(
                    painter = rememberImagePainter(
                        data = fotoUri,
                        builder = {
                            crossfade(true)
                            //placeholder(R.drawable.placeholder_image) // Si deseas agregar un placeholder
                        }
                    ),
                    contentDescription = "Imagen seleccionada en pantalla completa",
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )

                // Botón para cerrar la foto en pantalla completa
                IconButton(
                    onClick = {
                        // Al hacer clic en el botón X, deseleccionar la foto
                        formRecepcionVm.fotoSeleccionada.value = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White // Cambia el color del ícono a blanco
                    )
                }
            }
        }

        Text("La ubicación es: lat: ${formRecepcionVm.latitud.value} y long: ${formRecepcionVm.longitud.value}")
        Button(onClick = {
            actualizarUbicacionOnClick()

        }) {
            Text("Actualizar Ubicación")
        }
        Spacer(Modifier.height(100.dp))
        MapaOsmUI(formRecepcionVm.latitud.value,
            formRecepcionVm.longitud.value)
    }
}

// Composable UI para la pantalla de la fotografía
@Composable
fun PantallaFotoUI(
    formRecepcionVm: FormRecepcionViewModel,
    appViewModel: CameraAppViewModel,
    cameraController: CameraController
) {
    val contexto = LocalContext.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                controller = cameraController
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    Button(onClick = {
        tomarFotografia(
            cameraController,
            crearArchivoImagen(contexto),
            contexto,
            formRecepcionVm
        ) {
            // Agregar la nueva foto a la lista
            formRecepcionVm.fotoRecepcionVarias.add(it)
            // Guardar la foto seleccionada para previsualización
            formRecepcionVm.fotoSeleccionada.value = it
            appViewModel.cambiarPantallaForm()
        }
    }) {
        Text("Tomar foto")
    }

    // Mostrar la previsualización de la foto capturada
    formRecepcionVm.fotoSeleccionada.value?.let { fotoUri ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Image(
                painter = BitmapPainter(uri2imageBitmap(fotoUri, contexto)),
                contentDescription = "Previsualización de la foto capturada",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Composable UI para mostrar un mapa
@Composable
fun MapaOsmUI(latitud:Double, longitud:Double) {
    val contexto = LocalContext.current
    AndroidView(
        factory = {
            MapView(it).also {
                it.setTileSource(TileSourceFactory.MAPNIK)
                Configuration.getInstance().userAgentValue =
                    contexto.packageName
            }
        }, update = {
            it.overlays.removeIf { true }
            it.invalidate()
            it.controller.setZoom(18.0)
            val geoPoint = GeoPoint(latitud, longitud)
            it.controller.animateTo(geoPoint)
            val marcador = Marker(it)
            marcador.position = geoPoint
            marcador.setAnchor(Marker.ANCHOR_CENTER,
                Marker.ANCHOR_CENTER)
            it.overlays.add(marcador)
        }
    )
}

fun crearArchivoImagen(contexto: Context): File = File(
    contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "${generarNombreSegunFechaHastaSegundo()}.jpg"
)




