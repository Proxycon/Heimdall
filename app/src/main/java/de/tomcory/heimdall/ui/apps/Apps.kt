package de.tomcory.heimdall.ui.apps

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.tomcory.heimdall.R
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.scanner.code.ScanManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppInfoList(paddingValues: PaddingValues, apps: List<App>) {
    LazyColumn(modifier = Modifier.padding(paddingValues)) {
        items(apps) {
            var showAppDetailDialog by remember { mutableStateOf(false) }
            AppListItem(
                app = it,
                modifier = Modifier.clickable {
                    showAppDetailDialog = true
                }
            )

            if(showAppDetailDialog) {
                Dialog(
                    onDismissRequest = { showAppDetailDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    NewAppDetailScreen(app = it, onDismissRequest = { showAppDetailDialog = false })
                }
            }
        }
    }
}

@Composable
fun AppListItem(app: App, modifier: Modifier) {
    ListItem(
        headlineContent = {
            if(!app.isInstalled) {
                StrikethroughText(text = app.label)
            } else {
                Text(text = app.label)
            }

        },
        supportingContent = {
            if(!app.isInstalled) {
                StrikethroughText(text = app.packageName)
            } else {
                Text(text = app.packageName)
            }
        },
        leadingContent = {
            val painter = if(app.icon == null) {
                painterResource(R.drawable.robot)
            } else {
                rememberDrawablePainter(drawable = app.icon)
            }

            val colorFilter = if(!app.isInstalled) {
                ColorFilter.colorMatrix(ColorMatrix().apply {
                    setToSaturation(0f) // setting saturation to 0 will convert image to grayscale
                })
            } else {
                null
            }

            Image(painter = painter, contentDescription = "App icon", modifier = Modifier.size(40.dp), colorFilter = colorFilter)
        },
        modifier = modifier
    )
}

@Composable
fun StrikethroughText(text: String, modifier: Modifier = Modifier) {
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            append(text)
        }
    }

    Text(text = annotatedString, modifier = modifier)
}

@Composable
fun AppsScreen(navController: NavHostController?) {
    val context = LocalContext.current
    var loadingApps by remember { mutableStateOf(true) }

    var apps by remember { mutableStateOf(listOf<App>()) }

    LaunchedEffect(key1 = null, block = {
        apps = getApps(context)
        loadingApps = false
    })

    Scaffold(
        topBar = {
            AppsTopBar()
        }
    ) {
        AnimatedVisibility(visible = loadingApps, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
                Text(text = "Loading apps...")
            }
        }

        AnimatedVisibility(visible = !loadingApps, enter = fadeIn(), exit = fadeOut()) {
            AppInfoList(it, apps = apps)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsTopBar() {
    var searchActive by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        SearchBar(
            placeholder = { Text(text = "Search apps") },
            query = "",
            onQueryChange = {},
            onSearch = {},
            active = searchActive,
            onActiveChange = { searchActive = it },
            leadingIcon = {
                if (searchActive) {
                    IconButton(onClick = { searchActive = false }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = ""
                        )
                    }
                } else {
                    IconButton(onClick = { searchActive = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = ""
                        )
                    }
                }
            },
            trailingIcon = {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = ""
                    )
                }
            },
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun AppsScreenPreview() {
    AppsScreen(null)
}

@Composable
fun AppInfoCard(app: App) {

    var isSelected by remember { mutableStateOf(false) }
    val surfaceColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, label = "surfaceColorSelected"
    )
    Row(
        modifier = Modifier
            .height(88.dp)
            .clickable {

            }
            .padding(16.dp, 12.dp, 24.dp, 12.dp)
            .fillMaxWidth()
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = app.icon),
            contentDescription = "App Icon",
            modifier = Modifier
                .size(56.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "16 Permissions (8 dangerous)",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun AppInfoCardPreview(){
    AppInfoCard(app = App("test.package.com", "TestApp", "0.1", 1 ))
}

suspend fun getApps(context: Context): List<App> = withContext(Dispatchers.IO) {
    val apps = HeimdallDatabase.instance?.appDao?.getAll() ?: listOf()
    apps.forEach {
        if(it.icon == null && it.isInstalled) {
            it.icon = ScanManager.getAppIcon(context, it.packageName)
        }
    }
    return@withContext apps
}


data class AppInfo(
    val packageName: String,
    val label: String,
    var icon: Drawable? = null,
    var flags: Int = 0
)