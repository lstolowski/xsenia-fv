package pl.xsenia.fv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.ksef.KsefEnvironment
import pl.xsenia.fv.ui.theme.XseniaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XseniaTheme {
                StartScreen(
                    environment = KsefEnvironment.valueOf(BuildConfig.KSEF_ENVIRONMENT),
                    versionName = BuildConfig.VERSION_NAME,
                )
            }
        }
    }
}

/** Temporary start screen confirming which app flavor and KSeF environment is running. */
@Composable
private fun StartScreen(environment: KsefEnvironment, versionName: String) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.environment_label, environment.name), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.version_label, versionName), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() {
    XseniaTheme {
        StartScreen(environment = KsefEnvironment.TEST, versionName = "0.1.0-test")
    }
}
