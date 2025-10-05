package firstapp.gwu.newsappproject


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import firstapp.gwu.newsappproject.ui.theme.NewsAppProjectTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import firstapp.gwu.newsappproject.ui.theme.NewsAppProjectTheme

class MainSearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsAppProjectTheme {
                MainSearchScreen()
                }
            }
        }
    }
//Saves the search term + categories
const val EXTRA_SEARCH_TERM = "extra_search_term"

@Composable
fun MainSearchScreen() {
    val context = LocalContext.current
    var searchTerm by rememberSaveable { mutableStateOf("") }
    val isSearchEnabled = searchTerm.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("News Search", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        //Search box
        OutlinedTextField(
            value = searchTerm,
            onValueChange = {searchTerm = it},
            label = {Text("Search term")},
            placeholder = {Text("search...")},
            singleLine = true,
            //keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            //keyboardActions = KeyboardActions()
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        //Search Button
        Button(
            onClick = {
                //Redirects to sources
                val intent = Intent(context, SourcesActivity::class.java)
                //Saves the search term
                intent.putExtra(EXTRA_SEARCH_TERM, searchTerm.trim())
                context.startActivity(intent)
            },
            enabled = isSearchEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }
        Spacer(Modifier.height(24.dp))
        //Map button
        Button(
            onClick = {
                Toast.makeText(context, "Map", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Map")
        }
        Spacer(Modifier.height(24.dp))
        //Top headlines button
        Button(
            onClick = {
                Toast.makeText(context, "Top Headlines", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Top Headlines")
        }
    }
}
