package firstapp.gwu.newsappproject
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import firstapp.gwu.newsappproject.ui.theme.NewsAppProjectTheme
import java.lang.Compiler.enable


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsAppProjectTheme {
                NewsApp(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun NewsApp(modifier: Modifier = Modifier) {
    var password by remember { mutableStateOf("") }
    var loginBtnState by remember {mutableStateOf(false)}
    val context= LocalContext.current
    val prefs=remember {context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)}
    var username by remember {
        mutableStateOf(prefs.getString("username", "") ?: "")
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        //verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = "NEWS APP")
        Spacer(Modifier.height(24.dp))

        //username
        OutlinedTextField(
            value = username,
            onValueChange = {newText -> username=newText},
            label = { Text("Enter username...") },
            singleLine = true,
            isError = username.isNotEmpty() && !(username.length >= 5)
            )
        Spacer(Modifier.height(12.dp))
        //password
        OutlinedTextField(
            value = password,
            onValueChange = {newText-> password=newText},
            label = { Text("Enter password...") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
        //Login button: only enabled when username and password are filled and long enough
        Button(onClick = {
            prefs.edit { putString("username", username) }
            val intent = Intent(context, MainSearchActivity::class.java)
            context.startActivity(intent)
            Toast.makeText(context, "Logged in!", Toast.LENGTH_SHORT).show()
        },
            enabled = checkUsernamePassword(username, password))
        { Text(text = "Login") }
    }

}
//Makes sure username has input >= 5 chars and password has input >= 8 chars
fun checkUsernamePassword(username: String, password:String): Boolean {
    val noBlanks = username.isNotBlank()&&password.isNotBlank()
    return noBlanks && username.length >= 5 && password.length >= 8
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NewsApp(
        modifier = Modifier.fillMaxSize()
    )
}