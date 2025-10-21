package firstapp.gwu.newsappproject

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import firstapp.gwu.newsappproject.ui.theme.NewsAppProjectTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

//article data
data class Article(
    val title: String,
    val source: String,
    val description: String,
    val url: String,
    val imageUrl: String?
)

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsAppProjectTheme {
                MapScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val apiKey = context.getString(R.string.news_api_key)
    val prefs = remember { context.getSharedPreferences("map_prefs", MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    //single OkHttpClient instance
    val client = remember {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder().addInterceptor(log).build()
    }

    //saves the camera/marker or defaults (got defaults from site)
    val savedLat = prefs.getFloat("lat", Float.NaN)
    val savedLng = prefs.getFloat("lng", Float.NaN)
    val savedZoom = prefs.getFloat("zoom", 2f)
    val initialLatLng =
        if (!savedLat.isNaN() && !savedLng.isNaN())
            LatLng(savedLat.toDouble(), savedLng.toDouble())
        else LatLng(20.0, 0.0)
    val initialZoom = if (savedZoom <= 0f) 2f else savedZoom

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, initialZoom)
    }
    var marker by remember {
        mutableStateOf<LatLng?>(if (!savedLat.isNaN() && !savedLng.isNaN()) initialLatLng else null)
    }
    var placeLabel by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    //reverse geocode a label for the selected point: help from geocoding source
    fun getLabel(latLng: LatLng): String {
        return try {
            @Suppress("DEPRECATION")
            android.location.Geocoder(context, Locale.getDefault())
                .getFromLocation(latLng.latitude, latLng.longitude, 1)
                ?.firstOrNull()
                ?.let { a ->
                    // Try city first, then county, then state/province, then country
                    val city = a.locality?.trim().orEmpty()
                    val county = a.subAdminArea?.trim().orEmpty()
                    val state = a.adminArea?.trim().orEmpty()
                    val country = a.countryName?.trim().orEmpty()

                    // ----- NEW RULES -----
                    // If the point is in the United States, prefer the STATE name for the search label.
                    val isUS = country.equals("United States", ignoreCase = true) ||
                            country.equals("United States of America", ignoreCase = true) ||
                            country.equals("USA", ignoreCase = true) ||
                            country.equals("US", ignoreCase = true)

                    when {
                        isUS && state.isNotBlank() -> state                 // <-- prefer state for U.S.
                        city.isNotBlank() -> city
                        county.isNotBlank() -> county
                        state.isNotBlank() -> state
                        country.isNotBlank() -> country
                        else -> String.format(Locale.US, "%.2f, %.2f", latLng.latitude, latLng.longitude)
                    }
                }
                ?: String.format(Locale.US, "%.2f, %.2f", latLng.latitude, latLng.longitude)
        } catch (_: Exception) {
            String.format(Locale.US, "%.2f, %.2f", latLng.latitude, latLng.longitude)
        }
    }


    //searches titles for the location label
    suspend fun getArticles(label: String): Pair<List<Article>, String?> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(label, Charsets.UTF_8.name())
        val url = "https://newsapi.org/v2/everything?qInTitle=$encoded&language=en&sortBy=publishedAt"
        val req = Request.Builder()
            .url(url)
            .header("X-Api-Key", apiKey)
            .get()
            .build()
        try {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                val root = JSONObject(body)
                val arr = root.optJSONArray("articles")
                if (arr == null || arr.length() == 0) {
                    emptyList<Article>() to null
                } else { //gets all the articles from that location
                    val list = buildList {
                        for (i in 0 until arr.length()) {
                            val a = arr.getJSONObject(i)
                            val src = a.optJSONObject("source")?.optString("name").orEmpty()
                            add(
                                Article(
                                    title = a.optString("title"),
                                    source = if (src.isBlank()) "(Unknown source)" else src,
                                    description = a.optString("description").ifBlank { a.optString("content") },
                                    url = a.optString("url"),
                                    imageUrl = a.optString("urlToImage").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                    list to null
                }
            }
        } catch (e: Exception) {
            emptyList<Article>() to ("Network error: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    //sets a marker then gets the news
    LaunchedEffect(Unit) {
        if (marker == null) {
            //default marker to DC
            val starter = LatLng(38.9072, -77.0369) // Washington, DC
            marker = starter
            placeLabel = getLabel(starter)
            camera.move(CameraUpdateFactory.newLatLngZoom(starter, 8f))
            prefs.edit()
                .putFloat("lat", starter.latitude.toFloat())
                .putFloat("lng", starter.longitude.toFloat())
                .putFloat("zoom", 8f)
                .apply()
        }
        marker?.let { latLng ->
            placeLabel = getLabel(latLng)
            isLoading = true; errorMsg = null
            val (list, err) = getArticles(placeLabel ?: "")
            isLoading = false
            if (err != null) errorMsg = err else articles = list
        }
    }
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A2540),
            Color(0xFF144E68),
            Color(0xFFE7F3FF)
        )
    )
    //top app bar with back button: from Android developers https://developer.android.com/develop/ui/compose/components/app-bars-navigate
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local News Map", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(gradient)
        ) {

            //displays the map
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp), // leave room for the header row
                cameraPositionState = camera,
                onMapLongClick = { latLng ->
                    //long-press: set marker, resolve label, animate camera, persist coords
                    marker = latLng
                    placeLabel = getLabel(latLng)
                    camera.move(CameraUpdateFactory.newLatLngZoom(latLng, 8f))
                    prefs.edit()
                        .putFloat("lat", latLng.latitude.toFloat())
                        .putFloat("lng", latLng.longitude.toFloat())
                        .putFloat("zoom", 8f)
                        .apply()
                    //gets local news for new location
                    isLoading = true; errorMsg = null
                    scope.launch {
                        val (list, err) = getArticles(placeLabel ?: "")
                        isLoading = false
                        if (err != null) errorMsg = err else articles = list
                    }
                }
            ) {
                marker?.let { Marker(state = MarkerState(it), title = placeLabel ?: "Selected") }
            }

            //displays the resulting articlees from that area
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xCCFFFFFF))
                    .padding(12.dp)
            ) {
                Text(
                    "Results for ${placeLabel ?: "—"}",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(6.dp))
                when {
                    articles.isEmpty() -> {
                        Text("No articles found.")
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(articles) { a ->
                                //card for each article
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            //opens the article in the browser
                                            if (a.url.isNotBlank()) {
                                                try {
                                                    context.startActivity(
                                                        Intent(
                                                            Intent.ACTION_VIEW,
                                                            android.net.Uri.parse(a.url)
                                                        )
                                                    )
                                                } catch (_: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Unable to open article",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE7F3FF), // pale blue to match gradient
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(a.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = a.title,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .padding(end = 12.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = a.title.ifBlank { "(No title)" },
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                a.source,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = a.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
