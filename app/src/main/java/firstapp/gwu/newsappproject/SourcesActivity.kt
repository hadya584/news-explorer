package firstapp.gwu.newsappproject
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import firstapp.gwu.newsappproject.ui.theme.NewsAppProjectTheme

class SourcesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val currentSearch = intent?.getStringExtra(EXTRA_SEARCH_TERM) ?: ""
        setContent {
            NewsAppProjectTheme {
                SourcesScreen(
                    initialSearchTerm = currentSearch
                )
            }
        }
    }
}


@Composable
fun SourcesScreen(initialSearchTerm: String) {
    val context = LocalContext.current
    var searchTerm by rememberSaveable{mutableStateOf(initialSearchTerm)}
    //Categories in dropdown
    val categories = listOf( "business", "entertainment", "general", "health", "science", "sports", "technology")
    var selectedCategory by rememberSaveable { mutableStateOf(categories.first()) }
    //Random sources
    val fakeSources = remember(selectedCategory) {
        listOf(
            SourceItem(id = "tc", name = "TechCrunch", description = "Startup and technology news"),
            SourceItem(id = "verge", name = "The Verge", description = "Tech, science, art, and culture"),
            SourceItem(id = "bbc", name = "BBC News", description = "Breaking news, features, analysis"),
            SourceItem(id = "reuters", name = "Reuters", description = "Global business and markets coverage")
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        //Shows what the user searched for in search activity
        Text(text = "Search for: \"$searchTerm\"", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        //Shows selected category
        CategoryDropdown(
            categories = categories,
            selected = selectedCategory,
            onSelected = {selectedCategory = it}
        )
        Spacer(Modifier.height(16.dp))
        Text("Sources", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        //Displays the random sources
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fakeSources) { src ->
                SourceCard(
                    item = src,
                    onClick = {
                        Toast.makeText(context, "Source Clicked", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        //Skip sources button
        Button(
            onClick = {
                Toast.makeText(context, "Skip", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip")
        }
    }


}
@Composable
fun CategoryDropdown(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Category: $selected")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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
//Card for each source
@Composable
fun SourceCard(item: SourceItem, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(item.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

data class SourceItem(
    val id: String,
    val name: String,
    val description: String
)

