package firstapp.gwu.newsappproject

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import firstapp.gwu.newsappproject.ui.theme.NewsAppProjectTheme
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

//extras to pass to results
const val EXTRA_SOURCE_ID = "extra_source_id"
const val EXTRA_SOURCE_NAME = "extra_source_name"

//source data
data class SourceItem(
    val id: String,
    val name: String,
    val description: String
)

class SourcesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //gets the search term from MainSearchActivity
        val currentSearch = intent?.getStringExtra(EXTRA_SEARCH_TERM) ?: ""
        setContent {
            NewsAppProjectTheme {
                SourcesScreen(
                    initialSearchTerm = currentSearch,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    initialSearchTerm: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apiKey = context.getString(R.string.news_api_key)

    val gradient = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)))
    }

    //dropdown categories from News API
    val categories = listOf("business", "entertainment", "general", "health", "science", "sports", "technology")
    var searchTerm by rememberSaveable { mutableStateOf(initialSearchTerm) }
    var selectedCategory by rememberSaveable { mutableStateOf(categories.first()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var sources by remember { mutableStateOf<List<SourceItem>>(emptyList()) }

    //OkHttp client
    val client = remember {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    //get new sources whenever the category changes
    LaunchedEffect(selectedCategory) {
        sources = emptyList()
        try {
            val body = withContext(kotlinx.coroutines.Dispatchers.IO) {
                val url = "https://newsapi.org/v2/top-headlines/sources?language=en&category=$selectedCategory"
                val request = Request.Builder()
                    .url(url)
                    .header("X-Api-Key", apiKey)
                    .get()
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}") as Throwable
                    resp.body?.string() ?: throw IOException("Empty body")
                }
            }

            val root = JSONObject(body)
            if (root.optString("status") != "ok") {
                errorMsg = root.optString("message", "API error")
            } else {
                val arr = root.optJSONArray("sources")
                val list = buildList {
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val s = arr.getJSONObject(i)
                            add(
                                SourceItem(
                                    id = s.optString("id").orEmpty(),
                                    name = s.optString("name").orEmpty(),
                                    description = s.optString("description").orEmpty()
                                )
                            )
                        }
                    }
                }
                sources = list
            }
        } catch (e: Exception) {
            errorMsg = e.localizedMessage ?: "Network error"
        }
    }


    //top app bar with back button
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Sources", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(inner)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    //show search term
                    Text(
                        text = "Search for: \"$searchTerm\"",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    //dropdown to select the category
                    CategoryDropdown(
                        categories = categories,
                        selected = selectedCategory,
                        onSelected = { selectedCategory = it }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Sources", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    when {
                        sources.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No sources for \"$selectedCategory\".")
                            }
                        }
                        else -> { //displays the sources
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(sources) { src ->
                                    SourceRowCard(
                                        item = src,
                                        onClick = {
                                            val intent = Intent(context, ResultsActivity::class.java)
                                                .putExtra(EXTRA_SEARCH_TERM, searchTerm.trim())
                                                .putExtra(EXTRA_SOURCE_ID, src.id)
                                                .putExtra(EXTRA_SOURCE_NAME, src.name)
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    //skip button go straight to results with just search term
                    Button(
                        onClick = {
                            val intent = Intent(context, ResultsActivity::class.java)
                                .putExtra(EXTRA_SEARCH_TERM, searchTerm.trim())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Skip") }
                }
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Category: $selected") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat) },
                    onClick = {
                        onSelected(cat)
                        expanded = false
                    }
                )
            }
        }
    }
}
//displays the sources and their data
@Composable
private fun SourceRowCard(item: SourceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7F3FF),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(4.dp))
            Text(item.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
