package firstapp.gwu.newsappproject

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import firstapp.gwu.newsappproject.ui.theme.NewsAppProjectTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.max

//result data
data class ResultArticle(
    val title: String,
    val sourceName: String,
    val description: String,
    val url: String,
    val imageUrl: String?
)

class ResultsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //gets inputs passed from Sources or MainSearch
        val term = intent?.getStringExtra(EXTRA_SEARCH_TERM).orEmpty()
        val sourceId = intent?.getStringExtra(EXTRA_SOURCE_ID)
        val sourceName = intent?.getStringExtra(EXTRA_SOURCE_NAME)
        setContent {
            NewsAppProjectTheme {
                ResultsScreen(
                    term = term,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    onBack = { finish() } //go back to previous screen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    term: String,
    sourceId: String?,
    sourceName: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apiKey = context.getString(R.string.news_api_key)
    //one OKHTTP client
    val client = remember {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder().addInterceptor(logging).build()
    }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var articles by remember { mutableStateOf<List<ResultArticle>>(emptyList()) }

    val gradient = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)))
    }

    //gets new results when search term changes
    LaunchedEffect(term, sourceId) {
        loading = true
        errorMsg = null
        articles = emptyList()
        try {
            val safeTerm = term.trim()
            val encoded = URLEncoder.encode(safeTerm, Charsets.UTF_8.name())
            val url = buildString {
                append("https://newsapi.org/v2/everything?q=").append(encoded)
                append("&language=en&sortBy=publishedAt")
                if (!sourceId.isNullOrBlank()) append("&sources=").append(sourceId)
            }
            val request = Request.Builder()
                .url(url)
                .header("X-Api-Key", apiKey) // don’t hardcode the key
                .get()
                .build()
            val parsed = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful || body.isEmpty()) {
                        throw IllegalStateException("HTTP ${resp.code}")
                    }
                    val root = JSONObject(body)
                    if (root.optString("status") != "ok") {
                        val msg = root.optString("message", "API error")
                        throw IllegalStateException(msg)
                    }
                    val arr = root.optJSONArray("articles")
                    val list = mutableListOf<ResultArticle>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val a = arr.getJSONObject(i)
                            val srcName = a.optJSONObject("source")?.optString("name").orEmpty()
                            list.add(
                                ResultArticle(
                                    title = a.optString("title").orEmpty(),
                                    sourceName = if (srcName.isNotBlank()) srcName else "(Unknown source)",
                                    description = a.optString("description")
                                        .ifBlank { a.optString("content") },
                                    url = a.optString("url").orEmpty(),
                                    imageUrl = a.optString("urlToImage").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                    list
                }
            }
            articles = parsed
            if (parsed.isEmpty()) {
                // Simple friendly empty-state message
                errorMsg = "No articles found for “$term”${if (!sourceName.isNullOrBlank()) " from $sourceName" else ""}."
            }
        } catch (e: Exception) {
            errorMsg = e.localizedMessage ?: "Network error"
            // optional quick toast so you notice while testing
            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
        } finally {
            loading = false
        }
    }

    //top bar with back button
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
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
            //card for sources
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
                    //includes the source name along with the search term if given
                    val header = if (sourceName.isNullOrBlank())
                        "Results for \"$term\""
                    else
                        "$sourceName results for \"$term\""
                    Text(header, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))

                    when {
                        else -> {
                            // Results list
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(articles) { a ->
                                    ResultArticleCard(a)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

//card for each article
@Composable
private fun ResultArticleCard(article: ResultArticle) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (article.url.isNotBlank()) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Unable to open article", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7F3FF),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(article.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = article.title,
                modifier = Modifier
                    .size(84.dp)
                    .padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title.ifBlank { "(No title)" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.sourceName,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
