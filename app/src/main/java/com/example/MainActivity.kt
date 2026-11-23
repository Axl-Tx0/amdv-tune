package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.configureAppCheck
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutUser
import com.example.data.AppDatabase
import com.example.data.CatalogRepository
import com.example.data.CloudSyncRepository
import com.example.data.CommunityAndJamRepository
import com.example.data.MusicPlaybackService
import com.example.data.PlaybackControllerHolder
import com.example.data.ThemePreferencesRepository
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.TrackUiModel
import com.example.model.UserPlaylist
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.AdminCatalogSheet
import com.example.ui.components.AlbumDetailScreen
import com.example.ui.components.AmdvReplayOverlay
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.components.ArtistDetailScreen
import com.example.ui.components.AudioEffectsAndTimerSheet
import com.example.ui.components.CreateOrRenamePlaylistDialog
import com.example.ui.components.DrivingModeOverlay
import com.example.ui.components.FullScreenPlayerOverlay
import com.example.ui.components.JamSessionSheet
import com.example.ui.components.JoinPlaylistByCodeDialog
import com.example.ui.components.LyricsDetailSheet
import com.example.ui.components.MiniPlayerBanner
import com.example.ui.components.PlaylistDetailScreen
import com.example.ui.components.QueueBottomSheet
import com.example.ui.components.ReleasesInboxSheet
import com.example.ui.components.SharePlaylistCodeDialog
import com.example.ui.components.TrackCommentsSheet
import com.example.ui.components.formatSleepCountdown
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.AmdvTuneTheme
import com.example.ui.theme.LocalAmdvPalette
import com.example.viewmodel.MusicViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "inicio"
const val ROUTE_SEARCH = "buscar"
const val ROUTE_LIBRARY = "biblioteca"
const val ROUTE_PROFILE = "perfil"
const val EXTRA_OPEN_FULL_SCREEN_PLAYER = "com.example.OPEN_FULL_SCREEN_PLAYER"
private const val ADMIN_EMAIL = "amdvtune@gmail.com"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navDestinations = listOf(
    NavDestination(ROUTE_HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_inicio"),
    NavDestination(ROUTE_SEARCH, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search, "nav_buscar"),
    NavDestination(ROUTE_LIBRARY, R.string.nav_library, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_biblioteca"),
    NavDestination(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_perfil")
)

class MainActivity : ComponentActivity() {
    private var openPlayerSignal by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must run before setContent, ViewModels, or any Firebase operations
        configureAppCheck(this, intent)
        if (intent?.getBooleanExtra(EXTRA_OPEN_FULL_SCREEN_PLAYER, false) == true) {
            openPlayerSignal++
        }
        enableEdgeToEdge()
        setContent {
            AmdvTuneApp(openPlayerSignal = openPlayerSignal)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_FULL_SCREEN_PLAYER, false)) {
            openPlayerSignal++
        }
    }
}

@Composable
fun AmdvTuneApp(openPlayerSignal: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MusicViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                MusicViewModel(
                    appContext = app.applicationContext,
                    database = AppDatabase.getInstance(app.applicationContext),
                    themeRepo = ThemePreferencesRepository(app.applicationContext),
                    playerController = PlaybackControllerHolder.getInstance(app.applicationContext)
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderImportState by viewModel.folderImportState.collectAsStateWithLifecycle()
    val lyricsLookupState by viewModel.lyricsLookupState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )
    val isAdmin = currentUser?.email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) == true

    var isAuthLoading by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var isFullScreenPlayerOpen by remember { mutableStateOf(false) }
    var notificationPermissionRequested by remember { mutableStateOf(false) }
    var lyricsSheetTrack by remember { mutableStateOf<TrackUiModel?>(null) }
    var selectedArtist by remember { mutableStateOf<CatalogArtist?>(null) }
    var selectedAlbum by remember { mutableStateOf<CatalogAlbum?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<TrackUiModel?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var audioFxInitialTab by remember { mutableStateOf<Int?>(null) }
    var showJamSheet by remember { mutableStateOf(false) }
    var showReplayOverlay by remember { mutableStateOf(false) }
    var showReleasesInbox by remember { mutableStateOf(false) }
    var showDrivingMode by remember { mutableStateOf(false) }
    var showTrackComments by remember { mutableStateOf(false) }
    var sharingPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var generatedPlaylistCode by remember { mutableStateOf<String?>(null) }
    var isPublishingShareCode by remember { mutableStateOf(false) }
    var showJoinPlaylistDialog by remember { mutableStateOf(false) }
    var showAdminCatalogSheet by remember { mutableStateOf(false) }
    var dismissedReleaseCode by remember { mutableStateOf<Long?>(null) }
    var forceShowUpdateDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted && playbackState.currentTrack != null) {
                MusicPlaybackService.sync(context, isPlaying = playbackState.isPlaying)
            }
        }
    )

    fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionRequested) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionRequested = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(openPlayerSignal) {
        if (openPlayerSignal > 0 && playbackState.currentTrack != null) {
            isFullScreenPlayerOpen = true
        }
    }

    fun playAndExpand(track: TrackUiModel) {
        ensureNotificationPermissionIfNeeded()
        viewModel.playTrack(track)
        isFullScreenPlayerOpen = true
    }

    fun playQueueAndExpand(track: TrackUiModel, queue: List<TrackUiModel>) {
        ensureNotificationPermissionIfNeeded()
        viewModel.playTracksQueue(track, queue)
        isFullScreenPlayerOpen = true
    }

    fun openArtistForTrack(track: TrackUiModel) {
        val matched = uiState.artists.firstOrNull {
            it.id == track.artistId || it.name.equals(track.artist, ignoreCase = true)
        }
        if (matched != null) {
            selectedAlbum = null
            selectedArtist = matched
        } else {
            lyricsSheetTrack = track
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    // Strict Auth-Gated Firestore Synchronization: listeners are ONLY attached when signed in
    // and torn down cleanly via DisposableEffect / key(uid, isAdmin) on sign-out or account switch.
    val signedInUser = currentUser
    if (signedInUser != null) {
        val uid = signedInUser.uid
        val displayName = signedInUser.displayName ?: signedInUser.email ?: "Usuario AMDV"
        key(uid, isAdmin) {
            DisposableEffect(uid, isAdmin) {
                val dbId = context.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val repo = CloudSyncRepository(db, Firebase.auth)
                val catalogRepo = CatalogRepository(context.applicationContext, db, Firebase.auth)
                val communityRepo = CommunityAndJamRepository(db, Firebase.auth)
                viewModel.attachAuthenticatedCloud(uid, displayName, isAdmin, repo, catalogRepo, communityRepo)
                onDispose {
                    viewModel.detachAuthenticatedCloud()
                }
            }
        }
    } else {
        LaunchedEffect(Unit) {
            showAdminCatalogSheet = false
            selectedArtist = null
            selectedAlbum = null
            selectedPlaylistId = null
        }
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasAudioPermission = granted
            if (granted) {
                viewModel.syncDeviceAudioLibrary(silent = false)
            } else {
                viewModel.showMessage("Activa el permiso de audio para sincronizar las canciones de tu dispositivo")
            }
        }
    )

    val legacyFolderSyncLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { treeUri ->
            if (treeUri != null) {
                viewModel.syncDeviceAudioFromFolder(treeUri)
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasAudioPermission) {
            viewModel.syncDeviceAudioLibrary(silent = true)
        }
    }

    fun triggerDeviceMusicSync() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            hasAudioPermission = granted
            if (granted) {
                viewModel.syncDeviceAudioLibrary(silent = false)
            } else {
                audioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            legacyFolderSyncLauncher.launch(null)
        }
    }

    val combinedMessage = playbackState.errorMessage ?: uiState.syncMessage
    LaunchedEffect(combinedMessage) {
        if (!combinedMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(combinedMessage)
            viewModel.clearMessage()
        }
    }

    AmdvTuneTheme(themeMode = uiState.themeMode) {
        val palette = LocalAmdvPalette.current
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

        fun navigateTab(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = palette.background,
                contentWindowInsets = WindowInsets.statusBars,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MiniPlayerBanner(
                                playbackState = playbackState,
                                onPlayPause = viewModel::togglePlayPause,
                                onNext = viewModel::playNext,
                                onBannerClick = {
                                    if (playbackState.currentTrack != null) {
                                        isFullScreenPlayerOpen = true
                                    }
                                },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onArtistShortcutClick = ::openArtistForTrack
                            )
                            HorizontalDivider(color = palette.border, thickness = 1.dp)
                            NavigationBar(
                                containerColor = palette.surface,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                navDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTab(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(dest.labelRes)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = palette.textPrimary,
                                            selectedTextColor = palette.textPrimary,
                                            indicatorColor = palette.surfaceElevated,
                                            unselectedIconColor = palette.textSecondary,
                                            unselectedTextColor = palette.textSecondary
                                        ),
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
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
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = palette.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            navDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = palette.textPrimary,
                                        selectedTextColor = palette.textPrimary,
                                        indicatorColor = palette.surfaceElevated,
                                        unselectedIconColor = palette.textSecondary,
                                        unselectedTextColor = palette.textSecondary
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 640.dp)
                        ) {
                            if (useNavigationRail) {
                                MiniPlayerBanner(
                                    playbackState = playbackState,
                                    onPlayPause = viewModel::togglePlayPause,
                                    onNext = viewModel::playNext,
                                    onBannerClick = {
                                        if (playbackState.currentTrack != null) {
                                            isFullScreenPlayerOpen = true
                                        }
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onArtistShortcutClick = ::openArtistForTrack
                                )
                            }

                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_HOME,
                                modifier = Modifier.weight(1f)
                            ) {
                                composable(ROUTE_HOME) {
                                    HomeScreen(
                                        tracks = uiState.tracks,
                                        offlineTracks = uiState.offlineTracks,
                                        recentTracks = uiState.recentTracks,
                                        playlists = uiState.playlists,
                                        artists = uiState.artists,
                                        albums = uiState.albums,
                                        latestReleaseAlbum = uiState.latestReleaseAlbum,
                                        dailyMixTracks = uiState.dailyMixTracks,
                                        trendingTracks = uiState.trendingTracks,
                                        activeJam = uiState.activeJam,
                                        playbackState = playbackState,
                                        isAdmin = isAdmin,
                                        hasUnreadReleases = uiState.hasUnreadReleases,
                                        onSearchBarClick = { navigateTab(ROUTE_SEARCH) },
                                        onOpenReleasesInbox = {
                                            showReleasesInbox = true
                                            viewModel.markReleasesInboxSeen()
                                        },
                                        onTrackClick = ::playAndExpand,
                                        onPlaylistClick = { selectedPlaylistId = it.id },
                                        onArtistClick = { selectedArtist = it },
                                        onAlbumClick = { selectedAlbum = it },
                                        onPlayAlbum = { album ->
                                            ensureNotificationPermissionIfNeeded()
                                            viewModel.playAlbumTracks(album)
                                            isFullScreenPlayerOpen = true
                                        },
                                        onPlayDailyMix = {
                                            ensureNotificationPermissionIfNeeded()
                                            viewModel.playDailyMix()
                                            isFullScreenPlayerOpen = true
                                        },
                                        onRegenerateDailyMix = viewModel::regenerateDailyMix,
                                        onOpenJamSheet = { showJamSheet = true },
                                        onOpenAdminPanel = { showAdminCatalogSheet = true }
                                    )
                                }
                                composable(ROUTE_SEARCH) {
                                    SearchScreen(
                                        query = uiState.searchQuery,
                                        tracks = uiState.filteredTracks,
                                        artists = uiState.artists,
                                        albums = uiState.albums,
                                        playbackState = playbackState,
                                        onQueryChange = viewModel::updateSearchQuery,
                                        onTrackClick = ::playAndExpand,
                                        onArtistClick = { selectedArtist = it },
                                        onAlbumClick = { selectedAlbum = it },
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onOpenLyrics = {
                                            lyricsSheetTrack = it
                                        },
                                        onDownloadTrack = viewModel::downloadTrackOffline,
                                        onRemoveDownload = viewModel::removeOfflineDownload,
                                        onAddToPlaylist = { addToPlaylistTrack = it },
                                        onPlayNextInQueue = viewModel::playNextInQueue,
                                        onAddToQueueEnd = viewModel::addToQueueEnd
                                    )
                                }
                                composable(ROUTE_LIBRARY) {
                                    LibraryScreen(
                                        deviceTracks = uiState.deviceTracks,
                                        offlineTracks = uiState.offlineTracks,
                                        allTracks = uiState.tracks,
                                        playlists = uiState.playlists,
                                        isSignedIn = currentUser != null,
                                        playbackState = playbackState,
                                        isAdmin = isAdmin,
                                        hasAudioPermission = hasAudioPermission,
                                        isSyncingDeviceMusic = uiState.isImporting,
                                        onOpenAdminPanel = { showAdminCatalogSheet = true },
                                        onSyncDeviceMusicClick = ::triggerDeviceMusicSync,
                                        onGoToSearchClick = { navigateTab(ROUTE_SEARCH) },
                                        onPlaylistClick = { selectedPlaylistId = it.id },
                                        onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                                        onJoinPlaylistByCodeClick = { showJoinPlaylistDialog = true },
                                        onTrackClick = ::playQueueAndExpand,
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onDeleteTrack = viewModel::deleteTrack,
                                        onOpenLyrics = {
                                            lyricsSheetTrack = it
                                        },
                                        onDownloadTrack = viewModel::downloadTrackOffline,
                                        onRemoveDownload = viewModel::removeOfflineDownload,
                                        onAddToPlaylist = { addToPlaylistTrack = it },
                                        onPlayNextInQueue = viewModel::playNextInQueue,
                                        onAddToQueueEnd = viewModel::addToQueueEnd
                                    )
                                }
                                composable(ROUTE_PROFILE) {
                                    val sleepTimerLabel = when {
                                        playbackState.sleepAfterCurrentTrack -> "Fin pista"
                                        playbackState.sleepTimerRemainingMs != null -> formatSleepCountdown(playbackState.sleepTimerRemainingMs ?: 0L)
                                        else -> null
                                    }
                                    ProfileScreen(
                                        currentUser = currentUser,
                                        isAdmin = isAdmin,
                                        isAuthLoading = isAuthLoading,
                                        authErrorMessage = authErrorMessage,
                                        selectedTheme = uiState.themeMode,
                                        shuffleEnabled = playbackState.shuffleEnabled,
                                        repeatOneEnabled = playbackState.repeatOneEnabled,
                                        autoContinueEnabled = playbackState.autoContinueEnabled,
                                        muteOnNotificationsEnabled = playbackState.muteOnNotificationsEnabled,
                                        isEqualizerEnabled = playbackState.audioEffects.enabled,
                                        crossfadeSeconds = playbackState.crossfadeSeconds,
                                        hasActiveTrack = playbackState.currentTrack != null,
                                        activeSleepTimerLabel = sleepTimerLabel,
                                        latestRelease = uiState.latestRelease,
                                        favoriteCount = uiState.favoriteCount,
                                        offlineCount = uiState.offlineCount,
                                        totalSongsCount = uiState.tracks.size,
                                        topTracks = uiState.topTracks,
                                        topArtists = uiState.topArtists,
                                        totalPlaysCount = uiState.totalPlaysCount,
                                        totalListeningMinutes = uiState.totalListeningMinutes,
                                        listeningStreakDays = uiState.listeningStreakDays,
                                        onSignInClick = {
                                            authErrorMessage = null
                                            isAuthLoading = true
                                            onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onAuthSuccess = { isAuthLoading = false },
                                                onAuthError = { msg ->
                                                    isAuthLoading = false
                                                    authErrorMessage = msg
                                                },
                                                scope = scope,
                                                onAuthCancelled = { isAuthLoading = false }
                                            )
                                        },
                                        onSignOutClick = {
                                            signOutUser(
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onBeforeSignOut = { viewModel.detachAuthenticatedCloud() }
                                            )
                                        },
                                        onSelectTheme = viewModel::setThemeMode,
                                        onToggleShuffle = viewModel::toggleShuffle,
                                        onToggleRepeat = viewModel::toggleRepeat,
                                        onToggleAutoContinue = viewModel::toggleAutoContinue,
                                        onToggleMuteOnNotifications = viewModel::toggleMuteOnNotifications,
                                        onOpenEqualizer = { audioFxInitialTab = 0 },
                                        onOpenSleepTimer = { audioFxInitialTab = 1 },
                                        onOpenDrivingMode = {
                                            if (playbackState.currentTrack != null) {
                                                showDrivingMode = true
                                            } else {
                                                viewModel.showMessage("Reproduce una canción primero para activar el Modo Conducción.")
                                            }
                                        },
                                        onOpenReplay = { showReplayOverlay = true },
                                        onTrackClick = ::playAndExpand,
                                        onOpenUpdateDialog = { forceShowUpdateDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val currentArtist = selectedArtist
            if (currentArtist != null) {
                val liveArtist = uiState.artists.firstOrNull { it.id == currentArtist.id } ?: currentArtist
                val artistFollowerCount = uiState.artistStats[liveArtist.id]?.followerCount ?: 0L
                val isFollowingArtist = uiState.followedArtistIds.contains(liveArtist.id)
                ArtistDetailScreen(
                    artist = liveArtist,
                    allArtists = uiState.artists,
                    albums = uiState.albums,
                    tracks = uiState.cloudTracks,
                    playbackState = playbackState,
                    isFollowing = isFollowingArtist,
                    followerCount = artistFollowerCount,
                    onToggleFollow = { viewModel.toggleFollowArtist(liveArtist) },
                    onDismiss = { selectedArtist = null },
                    onSelectRelatedArtist = { nextArtist ->
                        selectedAlbum = null
                        selectedArtist = nextArtist
                    },
                    onAlbumClick = { album ->
                        selectedAlbum = album
                    },
                    onTrackClick = ::playQueueAndExpand,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenLyrics = {
                        lyricsSheetTrack = it
                    },
                    onDownloadTrack = viewModel::downloadTrackOffline,
                    onRemoveDownload = viewModel::removeOfflineDownload,
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::playNext,
                    onExpandPlayer = {
                        if (playbackState.currentTrack != null) {
                            isFullScreenPlayerOpen = true
                        }
                    },
                    onAddToPlaylist = { addToPlaylistTrack = it },
                    onPlayNextInQueue = viewModel::playNextInQueue,
                    onAddToQueueEnd = viewModel::addToQueueEnd
                )
            }

            val currentAlbum = selectedAlbum
            if (currentAlbum != null) {
                val liveAlbum = uiState.albums.firstOrNull { it.id == currentAlbum.id } ?: currentAlbum
                AlbumDetailScreen(
                    album = liveAlbum,
                    allArtists = uiState.artists,
                    tracks = uiState.cloudTracks,
                    playbackState = playbackState,
                    onDismiss = { selectedAlbum = null },
                    onOpenArtist = { art ->
                        selectedAlbum = null
                        selectedArtist = art
                    },
                    onDownloadAlbum = viewModel::downloadAlbumOffline,
                    onTrackClick = ::playQueueAndExpand,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenLyrics = {
                        lyricsSheetTrack = it
                    },
                    onDownloadTrack = viewModel::downloadTrackOffline,
                    onRemoveDownload = viewModel::removeOfflineDownload,
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::playNext,
                    onExpandPlayer = {
                        if (playbackState.currentTrack != null) {
                            isFullScreenPlayerOpen = true
                        }
                    },
                    onAddToPlaylist = { addToPlaylistTrack = it },
                    onPlayNextInQueue = viewModel::playNextInQueue,
                    onAddToQueueEnd = viewModel::addToQueueEnd
                )
            }

            val openPlaylist = selectedPlaylistId?.let { pid ->
                uiState.playlists.firstOrNull { it.id == pid }
            }
            if (openPlaylist != null) {
                PlaylistDetailScreen(
                    playlist = openPlaylist,
                    allTracks = uiState.tracks,
                    playbackState = playbackState,
                    onDismiss = { selectedPlaylistId = null },
                    onRenameClick = { renamingPlaylist = openPlaylist },
                    onDeleteClick = {
                        viewModel.deletePlaylist(openPlaylist.id) {
                            selectedPlaylistId = null
                        }
                    },
                    onDownloadPlaylist = { viewModel.downloadPlaylistOffline(openPlaylist) },
                    onTrackClick = ::playQueueAndExpand,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onRemoveFromPlaylist = { trackId ->
                        viewModel.removeTrackFromPlaylist(openPlaylist, trackId)
                    },
                    onDownloadTrack = viewModel::downloadTrackOffline,
                    onRemoveDownload = viewModel::removeOfflineDownload,
                    onPlayNextInQueue = viewModel::playNextInQueue,
                    onAddToQueueEnd = viewModel::addToQueueEnd,
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::playNext,
                    onExpandPlayer = {
                        if (playbackState.currentTrack != null) {
                            isFullScreenPlayerOpen = true
                        }
                    },
                    onShareClick = {
                        generatedPlaylistCode = null
                        sharingPlaylist = openPlaylist
                    }
                )
            }

            if (isAdmin && showAdminCatalogSheet) {
                AdminCatalogSheet(
                    artists = uiState.artists,
                    albums = uiState.albums,
                    cloudTracks = uiState.cloudTracks,
                    rawCatalogTracks = uiState.rawCatalogTracks,
                    moderationComments = uiState.moderationComments,
                    latestRelease = uiState.latestRelease,
                    folderImportState = folderImportState,
                    isUploading = uiState.isCatalogUploading,
                    uploadProgress = uiState.catalogUploadProgress,
                    onDismiss = { showAdminCatalogSheet = false },
                    onScanFolderTree = viewModel::scanLocalFolderTree,
                    onScanAudioUris = viewModel::scanMultipleLocalAudios,
                    onClearFolderPreview = viewModel::clearFolderPreview,
                    onPublishFolderAlbum = viewModel::adminUploadFolderAlbum,
                    onCreateArtist = viewModel::adminCreateArtist,
                    onCreateAlbum = viewModel::adminCreateAlbum,
                    onPublishTrack = viewModel::adminPublishTrack,
                    onDeleteArtist = viewModel::adminDeleteCatalogArtist,
                    onDeleteAlbum = viewModel::adminDeleteCatalogAlbum,
                    onDeleteTrack = viewModel::adminDeleteCatalogTrack,
                    onUpdateArtist = viewModel::adminUpdateArtist,
                    onUpdateAlbum = viewModel::adminUpdateAlbum,
                    onUpdateTrack = viewModel::adminUpdateTrack,
                    onPublishAppUpdate = viewModel::adminPublishAppUpdate,
                    onDeleteComment = viewModel::deleteTrackComment
                )
            }

            val pendingRelease = uiState.latestRelease
            val shouldShowUpdateDialog = pendingRelease != null &&
                pendingRelease.hasNewerVersion(BuildConfig.VERSION_CODE) &&
                (forceShowUpdateDialog || pendingRelease.mandatory || dismissedReleaseCode != pendingRelease.latestVersionCode)
            if (shouldShowUpdateDialog && pendingRelease != null) {
                AppUpdateAvailableDialog(
                    release = pendingRelease,
                    onDismiss = {
                        forceShowUpdateDialog = false
                        dismissedReleaseCode = pendingRelease.latestVersionCode
                    }
                )
            }

            val playingTrackId = playbackState.currentTrack?.id
            FullScreenPlayerOverlay(
                visible = isFullScreenPlayerOpen,
                playbackState = playbackState,
                isLyricsLoading = playingTrackId != null && lyricsLookupState.loadingTrackId == playingTrackId,
                lyricsStatusMessage = if (playingTrackId != null && lyricsLookupState.statusTrackId == playingTrackId) {
                    lyricsLookupState.statusMessage
                } else null,
                lyricsOffsetMs = playingTrackId?.let { uiState.lyricsOffsetsMs[it] } ?: 0L,
                communityStats = playingTrackId?.let { uiState.communityStats[it] },
                userReaction = playingTrackId?.let { uiState.userReactions[it] },
                activeJam = uiState.activeJam,
                trackComments = uiState.currentTrackComments,
                onCollapse = { isFullScreenPlayerOpen = false },
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::playNext,
                onPrevious = viewModel::playPrevious,
                onSeekTo = viewModel::seekTo,
                onToggleShuffle = viewModel::toggleShuffle,
                onToggleRepeat = viewModel::toggleRepeat,
                onToggleMuteOnNotifications = viewModel::toggleMuteOnNotifications,
                onToggleFavorite = viewModel::toggleFavorite,
                onDownloadTrack = viewModel::downloadTrackOffline,
                onRemoveDownload = viewModel::removeOfflineDownload,
                onEditLyrics = {
                    lyricsSheetTrack = it
                },
                onRequestAutoLyrics = { targetTrack, force ->
                    viewModel.fetchLyricsFromLrcLib(targetTrack, forceRefresh = force)
                },
                onAdjustLyricsOffset = viewModel::adjustLyricsOffset,
                onResetLyricsOffset = viewModel::resetLyricsOffset,
                onToggleReaction = viewModel::toggleTrackReaction,
                onOpenJamSheet = { showJamSheet = true },
                onOpenComments = { showTrackComments = true },
                onOpenDrivingMode = { showDrivingMode = true },
                onOpenArtist = { targetTrack ->
                    isFullScreenPlayerOpen = false
                    openArtistForTrack(targetTrack)
                },
                onAddToPlaylist = { addToPlaylistTrack = it },
                onOpenQueue = { showQueueSheet = true },
                onOpenEqualizer = { audioFxInitialTab = 0 },
                onOpenSleepTimer = { audioFxInitialTab = 1 }
            )

            DrivingModeOverlay(
                visible = showDrivingMode,
                playbackState = playbackState,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::playNext,
                onPrevious = viewModel::playPrevious,
                onToggleFavorite = viewModel::toggleFavorite,
                onClose = { showDrivingMode = false }
            )

            AmdvReplayOverlay(
                visible = showReplayOverlay,
                userName = currentUser?.displayName ?: currentUser?.email ?: "AMDV-TUNE",
                totalPlaysCount = uiState.totalPlaysCount,
                totalListeningMinutes = uiState.totalListeningMinutes,
                listeningStreakDays = uiState.listeningStreakDays,
                favoriteCount = uiState.favoriteCount,
                offlineCount = uiState.offlineCount,
                playlistCount = uiState.playlists.size,
                topTracks = uiState.topTracks,
                topArtists = uiState.topArtists,
                onPlayTrack = ::playAndExpand,
                onDismiss = { showReplayOverlay = false }
            )

            if (showJamSheet) {
                JamSessionSheet(
                    activeJam = uiState.activeJam,
                    isHost = uiState.isJamHost,
                    isSignedIn = currentUser != null,
                    allTracks = uiState.tracks,
                    currentTrack = playbackState.currentTrack,
                    onCreateRoom = viewModel::createJamSession,
                    onJoinRoom = viewModel::joinJamSession,
                    onLeaveOrCloseRoom = viewModel::leaveJamSession,
                    onAddTrackToJam = viewModel::addTrackToActiveJam,
                    onPlayJamTrack = { tr ->
                        viewModel.playTrack(tr)
                    },
                    onDismiss = { showJamSheet = false }
                )
            }

            val plToShare = sharingPlaylist
            if (plToShare != null) {
                SharePlaylistCodeDialog(
                    playlist = plToShare,
                    generatedCode = generatedPlaylistCode,
                    isPublishing = isPublishingShareCode,
                    onPublish = { collaborative ->
                        isPublishingShareCode = true
                        viewModel.sharePlaylistWithCode(plToShare, collaborative) { code ->
                            isPublishingShareCode = false
                            if (code != null) {
                                generatedPlaylistCode = code
                            }
                        }
                    },
                    onDismiss = {
                        sharingPlaylist = null
                        generatedPlaylistCode = null
                    }
                )
            }

            if (showJoinPlaylistDialog) {
                JoinPlaylistByCodeDialog(
                    onDismiss = { showJoinPlaylistDialog = false },
                    onJoin = { code ->
                        showJoinPlaylistDialog = false
                        viewModel.joinSharedPlaylistByCode(code)
                    }
                )
            }

            if (showQueueSheet) {
                QueueBottomSheet(
                    playbackState = playbackState,
                    onDismiss = { showQueueSheet = false },
                    onPlayIndex = { idx -> viewModel.playQueueIndex(idx) },
                    onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                    onRemoveItem = { idx -> viewModel.removeFromQueue(idx) }
                )
            }

            val fxTab = audioFxInitialTab
            if (fxTab != null) {
                AudioEffectsAndTimerSheet(
                    playbackState = playbackState,
                    initialTab = fxTab,
                    onDismiss = { audioFxInitialTab = null },
                    onToggleEqualizer = viewModel::setEqualizerEnabled,
                    onSelectPreset = viewModel::selectEqualizerPreset,
                    onChangeBandLevel = viewModel::setEqualizerBandLevel,
                    onChangeBassBoost = viewModel::setBassBoostStrength,
                    onChangeCrossfadeSeconds = viewModel::setCrossfadeSeconds,
                    onSetSleepTimerMinutes = viewModel::setSleepTimerMinutes,
                    onSetSleepAfterTrack = viewModel::setSleepAfterCurrentTrack,
                    onCancelSleepTimer = viewModel::cancelSleepTimer
                )
            }

            if (showReleasesInbox) {
                ReleasesInboxSheet(
                    recentAlbums = uiState.recentReleaseAlbums,
                    followedArtistIds = uiState.followedArtistIds,
                    onDismiss = { showReleasesInbox = false },
                    onOpenAlbum = { album ->
                        showReleasesInbox = false
                        selectedAlbum = album
                    },
                    onPlayAlbum = { album ->
                        showReleasesInbox = false
                        ensureNotificationPermissionIfNeeded()
                        viewModel.playAlbumTracks(album)
                        isFullScreenPlayerOpen = true
                    }
                )
            }

            val commentsTrack = playbackState.currentTrack
            if (showTrackComments && commentsTrack != null) {
                TrackCommentsSheet(
                    track = commentsTrack,
                    currentPositionMs = playbackState.positionMs,
                    comments = uiState.currentTrackComments,
                    currentUserId = currentUser?.uid,
                    isAdmin = isAdmin,
                    onDismiss = { showTrackComments = false },
                    onSeekTo = { targetMs ->
                        viewModel.seekTo(targetMs)
                    },
                    onAddComment = { tsMs, text, onDone ->
                        viewModel.addTrackComment(commentsTrack, tsMs, text, onDone)
                    },
                    onDeleteComment = viewModel::deleteTrackComment
                )
            }

            val targetPlaylistTrack = addToPlaylistTrack
            if (targetPlaylistTrack != null) {
                AddToPlaylistSheet(
                    track = targetPlaylistTrack,
                    playlists = uiState.playlists,
                    allTracks = uiState.tracks,
                    isSignedIn = currentUser != null,
                    onDismiss = { addToPlaylistTrack = null },
                    onToggleTrackInPlaylist = { pl ->
                        viewModel.toggleTrackInPlaylist(pl, targetPlaylistTrack)
                    },
                    onCreateNewPlaylist = { name, desc ->
                        viewModel.createPlaylist(
                            name = name,
                            description = desc,
                            initialTrackId = targetPlaylistTrack.id
                        )
                    }
                )
            }

            if (showCreatePlaylistDialog) {
                CreateOrRenamePlaylistDialog(
                    title = "Nueva playlist",
                    confirmLabel = "Crear",
                    onDismiss = { showCreatePlaylistDialog = false },
                    onConfirm = { name, desc ->
                        showCreatePlaylistDialog = false
                        viewModel.createPlaylist(name = name, description = desc)
                    }
                )
            }

            val playlistToRename = renamingPlaylist
            if (playlistToRename != null) {
                CreateOrRenamePlaylistDialog(
                    title = "Editar playlist",
                    initialName = playlistToRename.name,
                    initialDescription = playlistToRename.description,
                    confirmLabel = "Guardar",
                    onDismiss = { renamingPlaylist = null },
                    onConfirm = { name, desc ->
                        renamingPlaylist = null
                        viewModel.renamePlaylist(playlistToRename.id, name, desc)
                    }
                )
            }

            val activeLyricsTrack = lyricsSheetTrack
            if (activeLyricsTrack != null) {
                val refreshed = uiState.tracks.firstOrNull { it.id == activeLyricsTrack.id } ?: activeLyricsTrack
                LyricsDetailSheet(
                    track = refreshed,
                    isLyricsLoading = lyricsLookupState.loadingTrackId == refreshed.id,
                    lyricsStatusMessage = if (lyricsLookupState.statusTrackId == refreshed.id) {
                        lyricsLookupState.statusMessage
                    } else null,
                    onDismiss = { lyricsSheetTrack = null },
                    onSaveLyrics = { updated ->
                        viewModel.updateLyrics(refreshed, updated)
                    },
                    onRequestAutoLyrics = { force ->
                        viewModel.fetchLyricsFromLrcLib(refreshed, forceRefresh = force)
                    }
                )
            }
        }
    }
}
