package firstapp.gwu.newsappproject

import android.content.Context.MODE_PRIVATE
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
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

//headlines data
data class NewsHeadline(
    val title: String,
    val sourceName: String,
    val description: String,
    val url: String,
    val imageUrl: String?
)

//networking: did some research and used chat to help
class TopHeadlinesManager {
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    //returns results if found or throws and error
    fun getTopHeadlines(category: String, page: Int, apiKey: String, country: String = "us"): Pair<List<NewsHeadline>, Int> {
        val url = "https://newsapi.org/v2/top-headlines" + "?country=$country&category=$category&page=$page"
        val request = Request.Builder()
            .url(url)
            .header("X-Api-Key", apiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            val root = JSONObject(body)
            val total = root.optInt("totalResults", 0)
            val arr = root.optJSONArray("articles") ?: return emptyList<NewsHeadline>() to total

            val items = buildList {
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONObject(i)
                    val sourceName = a.optJSONObject("source")?.optString("name").orEmpty()
                    add(
                        NewsHeadline(
                            title = a.optString("title").orEmpty(),
                            sourceName = sourceName.ifEmpty { "(Unknown source)" },
                            description = a.optString("description").ifBlank { a.optString("content") },
                            url = a.optString("url").orEmpty(),
                            imageUrl = a.optString("urlToImage").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            return items to total
        }
    }
}

class TopHeadlinesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsAppProjectTheme {
                TopHeadlinesScreen(onBack = { finish() })
            }
        }
    }
}

private const val TH_PREFS = "top_headlines_prefs"
private const val TH_KEY_CATEGORY = "key_category"

//displays the list of headlines
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopHeadlinesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(TH_PREFS, MODE_PRIVATE) }
    val apiKey = context.getString(R.string.news_api_key)
    val manager = remember { TopHeadlinesManager() }
    //categories from news api
    val categories = listOf("business", "entertainment", "general", "health", "science", "sports", "technology")
    //restores the last category
    var selectedCategory by rememberSaveable {
        mutableStateOf(prefs.getString(TH_KEY_CATEGORY, "general")?.takeIf { it in categories } ?: "general")
    }
    var currentPage by rememberSaveable { mutableStateOf(1) } // 1-based
    var maxPages by rememberSaveable { mutableStateOf(1) }
    var articles by remember { mutableStateOf<List<NewsHeadline>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val gradient = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)))
    }

    //reloads everytime category or page changes
    LaunchedEffect(selectedCategory, currentPage) {
        isLoading = true
        errorMsg = null
        try {
            val (items, total) = withContext(Dispatchers.IO) {
                manager.getTopHeadlines(category = selectedCategory, page = currentPage, apiKey = apiKey)
            }
            articles = items
            val computed = min(ceil(total / 20.0).toInt(), 5)
            maxPages = if (computed < 1) 1 else computed
            if (items.isEmpty() && total == 0) {
                errorMsg = "No results for “$selectedCategory”."
            }
        } catch (e: Exception) {
            errorMsg = e.message ?: "Unexpected error"
            articles = emptyList()
            maxPages = 1
        } finally {
            isLoading = false
        }
    }

    //top app bar with back button: from Android developers https://developer.android.com/develop/ui/compose/components/app-bars-navigate
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Top Headlines", fontWeight = FontWeight.SemiBold) },
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
                    //category dropdown
                    HeadlinesCategoryDropdown(
                        categories = categories,
                        selected = selectedCategory,
                        onSelected = { newCat ->
                            if (newCat != selectedCategory) {
                                selectedCategory = newCat
                                prefs.edit().putString(TH_KEY_CATEGORY, newCat).apply()
                                currentPage = 1 // reset paging
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    //controls page scrolling
                    Pages(
                        current = currentPage,
                        max = maxPages,
                        onPrev = { if (currentPage > 1) currentPage -= 1 },
                        onNext = { if (currentPage < maxPages) currentPage += 1 }
                    )
                    Spacer(Modifier.height(8.dp))
                    //displays all the headlines with the correct category
                    when {
                        errorMsg != null -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Error: $errorMsg")
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { currentPage = currentPage }) { Text("Retry") }
                            }
                        }
                        articles.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No articles found.")
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(articles) { a -> ArticleCard(a) }
                            }
                        }
                    }
                }
            }
        }
    }
}

//dropdown of categories
@Composable
private fun HeadlinesCategoryDropdown(
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
        ) {
            Text("Category: $selected")
        }
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

//controls page scrolling
@Composable
private fun Pages(
    current: Int,
    max: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = onPrev, enabled = current > 1, shape = RoundedCornerShape(12.dp)) {
            Text("Previous")
        }
        Text("Page $current of $max", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onNext, enabled = current < max, shape = RoundedCornerShape(12.dp)) {
            Text("Next")
        }
    }
}

//displays each top headline article in a card
@Composable
private fun ArticleCard(article: NewsHeadline) {
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
            containerColor = Color(0xFFE7F3FF),                  // light blue background
            contentColor = MaterialTheme.colorScheme.onSurface   // normal content color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            //from chat: trouble displaying images from the API
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
            //displays all the article information
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
