package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppSettings
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.StudioMode
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.signOutGoogle
import com.example.ui.components.WelcomeAuthScreen
import com.example.ui.screens.AiDesignAssistantScreen
import com.example.ui.screens.BackupAndRestoreScreen
import com.example.ui.screens.DashboardModuleRoute
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportCenterScreen
import com.example.ui.screens.MaterialCalculatorScreen
import com.example.ui.screens.MyDesignsScreen
import com.example.ui.screens.SettingsAndTutorialsScreen
import com.example.ui.screens.SharedDesignsScreen
import com.example.ui.screens.StudioWorkspaceScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.CsiHomeCraftTheme
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class AppRoute(val title: String) {
    DASHBOARD("CSI • Creation S Irfan"),
    STUDIO("Studio Workspace"),
    MY_DESIGNS("My Designs"),
    TEMPLATES("Architectural Templates"),
    CALCULATOR("Material Calculator"),
    EXPORT_CENTER("Export Centre"),
    SHARED_DESIGNS("Share & Collaborate"),
    BACKUP_RESTORE("Google Backup & Restore"),
    AI_ASSISTANT("AI Design Assistant"),
    SETTINGS("Settings & Tutorials")
}

private data class PrimaryNavTab(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val prefsRepo = UserPreferencesRepository(applicationContext)

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(context) }

            val currentUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
            val settings by prefsRepo.settingsFlow
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            LaunchedEffect(Unit) {
                if (Firebase.auth.currentUser == null) {
                    attemptAutoSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {},
                        onUnauthenticated = {},
                        scope = scope
                    )
                }
            }

            CsiHomeCraftTheme(darkTheme = settings.darkTheme) {
                if (currentUser == null && !settings.offlineModeChosen) {
                    WelcomeAuthScreen(
                        onSignedIn = {
                            scope.launch { prefsRepo.setOfflineModeChosen(false) }
                        },
                        onContinueOffline = {
                            scope.launch { prefsRepo.setOfflineModeChosen(true) }
                        }
                    )
                } else {
                    val isSignedIn = currentUser != null
                    val ownerUid = currentUser?.uid ?: "local_offline"
                    val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "S Irfan"
                    val databaseId = remember { context.getString(R.string.firestore_database_id) }

                    val studioViewModel: StudioViewModel = viewModel(
                        key = ownerUid,
                        factory = StudioViewModel.factory(
                            context = context,
                            databaseId = databaseId,
                            ownerUid = ownerUid,
                            isSignedIn = isSignedIn,
                            userDisplayName = displayName
                        )
                    )

                    CsiHomeCraftShell(
                        viewModel = studioViewModel,
                        onRequestGoogleSignIn = {
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    scope.launch { prefsRepo.setOfflineModeChosen(false) }
                                    studioViewModel.showMessage("Signed in with Google Cloud")
                                },
                                onAuthError = { msg ->
                                    studioViewModel.showMessage(msg)
                                },
                                scope = scope
                            )
                        },
                        onDisconnectGoogle = {
                            signOutGoogle(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = {
                                    scope.launch { prefsRepo.setOfflineModeChosen(false) }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsiHomeCraftShell(
    viewModel: StudioViewModel,
    onRequestGoogleSignIn: () -> Unit,
    onDisconnectGoogle: () -> Unit
) {
    var currentRoute by remember { mutableStateOf(AppRoute.DASHBOARD) }
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    BackHandler(enabled = currentRoute != AppRoute.DASHBOARD) {
        currentRoute = AppRoute.DASHBOARD
    }

    val primaryTabs = remember {
        listOf(
            PrimaryNavTab(AppRoute.DASHBOARD, "Home", Icons.Filled.Dashboard),
            PrimaryNavTab(AppRoute.STUDIO, "Studio", Icons.Filled.Architecture),
            PrimaryNavTab(AppRoute.MY_DESIGNS, "Designs", Icons.Filled.FolderOpen),
            PrimaryNavTab(AppRoute.CALCULATOR, "Estimate", Icons.Filled.Calculate),
            PrimaryNavTab(AppRoute.AI_ASSISTANT, "AI", Icons.Filled.AutoAwesome)
        )
    }

    val navigateFromDashboard: (DashboardModuleRoute, StudioMode?) -> Unit = { modRoute, studioMode ->
        if (studioMode != null) {
            viewModel.setStudioMode(studioMode)
        }
        currentRoute = when (modRoute) {
            DashboardModuleRoute.NEW_PROJECT,
            DashboardModuleRoute.DRAWING_2D,
            DashboardModuleRoute.DESIGNER_3D,
            DashboardModuleRoute.INTERIOR,
            DashboardModuleRoute.ELECTRICAL,
            DashboardModuleRoute.PLUMBING -> AppRoute.STUDIO
            DashboardModuleRoute.MY_DESIGNS -> AppRoute.MY_DESIGNS
            DashboardModuleRoute.CALCULATOR -> AppRoute.CALCULATOR
            DashboardModuleRoute.TEMPLATES -> AppRoute.TEMPLATES
            DashboardModuleRoute.SHARED_DESIGNS -> AppRoute.SHARED_DESIGNS
            DashboardModuleRoute.BACKUP_RESTORE -> AppRoute.BACKUP_RESTORE
            DashboardModuleRoute.EXPORT_CENTER -> AppRoute.EXPORT_CENTER
            DashboardModuleRoute.AI_ASSISTANT -> AppRoute.AI_ASSISTANT
            DashboardModuleRoute.SETTINGS -> AppRoute.SETTINGS
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        if (currentRoute != AppRoute.DASHBOARD) {
                            IconButton(
                                onClick = { currentRoute = AppRoute.DASHBOARD },
                                modifier = Modifier.testTag("nav_back_dashboard")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Dashboard"
                                )
                            }
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = currentRoute.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = activeProject?.name?.let { "$it • Creation S Irfan" }
                                ?: "HOMECRAFT 3D Architectural Suite"
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { currentRoute = AppRoute.SHARED_DESIGNS },
                            modifier = Modifier.testTag("topbar_share_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share Design")
                        }
                        IconButton(
                            onClick = { currentRoute = AppRoute.EXPORT_CENTER },
                            modifier = Modifier.testTag("topbar_export_button")
                        ) {
                            Icon(Icons.Filled.IosShare, contentDescription = "Export Centre")
                        }
                        if (viewModel.isSignedIn) {
                            IconButton(
                                onClick = onDisconnectGoogle,
                                modifier = Modifier.testTag("topbar_signout_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                            }
                        } else {
                            IconButton(
                                onClick = onRequestGoogleSignIn,
                                modifier = Modifier.testTag("topbar_signin_button")
                            ) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = "Sign in with Google")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        primaryTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { currentRoute = tab.route },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("bottom_nav_${tab.route.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        primaryTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { currentRoute = tab.route },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_nav_${tab.route.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentRoute) {
                        AppRoute.DASHBOARD -> DashboardScreen(
                            activeProject = activeProject,
                            projectCount = allProjects.count { !it.isDeleted },
                            isSignedIn = viewModel.isSignedIn,
                            userDisplayName = viewModel.userDisplayName,
                            onCreateNewProject = { name, cat -> viewModel.createNewProject(name, cat) },
                            onNavigateModule = navigateFromDashboard
                        )
                        AppRoute.STUDIO -> StudioWorkspaceScreen(viewModel = viewModel)
                        AppRoute.MY_DESIGNS -> MyDesignsScreen(
                            viewModel = viewModel,
                            onOpenProjectInStudio = { proj ->
                                viewModel.selectProject(proj)
                                currentRoute = AppRoute.STUDIO
                            }
                        )
                        AppRoute.TEMPLATES -> TemplatesScreen(
                            onSelectTemplate = { builder ->
                                viewModel.createFromTemplate(builder)
                                currentRoute = AppRoute.STUDIO
                            }
                        )
                        AppRoute.CALCULATOR -> MaterialCalculatorScreen(viewModel = viewModel)
                        AppRoute.EXPORT_CENTER -> ExportCenterScreen(viewModel = viewModel)
                        AppRoute.SHARED_DESIGNS -> SharedDesignsScreen(
                            viewModel = viewModel,
                            onRequestGoogleSignIn = onRequestGoogleSignIn,
                            onOpenSharedProject = { sharedProj ->
                                viewModel.selectProject(sharedProj)
                                currentRoute = AppRoute.STUDIO
                            }
                        )
                        AppRoute.BACKUP_RESTORE -> BackupAndRestoreScreen(
                            viewModel = viewModel,
                            onRequestGoogleSignIn = onRequestGoogleSignIn,
                            onDisconnectGoogle = onDisconnectGoogle
                        )
                        AppRoute.AI_ASSISTANT -> AiDesignAssistantScreen(viewModel = viewModel)
                        AppRoute.SETTINGS -> SettingsAndTutorialsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
