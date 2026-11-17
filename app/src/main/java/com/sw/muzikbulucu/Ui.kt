package com.sw.muzikbulucu

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Green = Color(0xFF22C55E)
val GreenDark = Color(0xFF15803D)

// ---------- Yardımcılar ----------

fun micOk(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

fun notifOk(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(ctx, T("Açılamadı", "Could not open"), Toast.LENGTH_SHORT).show()
    }
}

fun continueBubble(ctx: Context) {
    if (!AndroidSettings.canDrawOverlays(ctx)) {
        Toast.makeText(
            ctx,
            T("Diğer uygulamaların üzerinde gösterme iznini ver, sonra geri dönüp anahtarı tekrar aç",
                "Allow display over other apps, then come back and turn the switch on again"),
            Toast.LENGTH_LONG
        ).show()
        ctx.startActivity(
            Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
        )
    } else {
        BubbleService.start(ctx)
    }
}

fun fmtDate(ts: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale(Store.lang)).format(Date(ts))

// ---------- Tema ----------

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = when (Store.theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) darkColorScheme(
        primary = Green, onPrimary = Color.Black,
        background = Color(0xFF07130C), onBackground = Color.White,
        surface = Color(0xFF10241A), onSurface = Color.White,
        surfaceVariant = Color(0xFF173222), onSurfaceVariant = Color(0xFFB7D3C0),
        secondaryContainer = Color(0xFF1F4A2E), onSecondaryContainer = Color.White
    ) else lightColorScheme(
        primary = GreenDark, onPrimary = Color.White,
        background = Color(0xFFF2FAF4), onBackground = Color(0xFF0B1F12),
        surface = Color.White, onSurface = Color(0xFF0B1F12),
        surfaceVariant = Color(0xFFE3F1E7), onSurfaceVariant = Color(0xFF3B5546),
        secondaryContainer = Color(0xFFBFE8CB), onSecondaryContainer = Color(0xFF0B1F12)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

// ---------- Kök ----------

@Composable
fun AppRoot(vm: MainVM) {
    AppTheme {
        BackHandler(vm.detail != null) { vm.detail = null }
        val items = listOf(
            Triple(T("Ana Sayfa", "Home"), Icons.Default.Home, 0),
            Triple(T("Geçmiş", "History"), Icons.Default.History, 1),
            Triple(T("Ayarlar", "Settings"), Icons.Default.Settings, 2)
        )
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    items.forEach { (label, icon, i) ->
                        NavigationBarItem(
                            selected = vm.tab == i && vm.detail == null,
                            onClick = { vm.tab = i; vm.detail = null },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Green
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                val d = vm.detail
                if (d != null) DetailScreen(d) { vm.detail = null }
                else when (vm.tab) {
                    0 -> HomeScreen(vm)
                    1 -> HistoryScreen(vm)
                    else -> SettingsScreen()
                }
            }
        }
    }
}

// ---------- Ortak parçalar ----------

@Composable
fun Cover(url: String, size: Dp, radius: Dp = 14.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(MaterialTheme.colorScheme.surfaceVariant),
        Alignment.Center
    ) {
        if (url.isBlank()) {
            Icon(Icons.Default.MusicNote, null, tint = Green, modifier = Modifier.size(size / 2))
        } else {
            AsyncImage(
                model = url, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ListenButton(state: UiState, level: Int, onClick: () -> Unit) {
    val listening = state is UiState.Listening
    val searching = state is UiState.Searching
    val inf = rememberInfiniteTransition(label = "pulse")
    val scale by inf.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "scale"
    )
    val fade by inf.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "fade"
    )
    Box(Modifier.size(270.dp), Alignment.Center) {
        if (listening || searching) {
            val extra = level / 32767f * 0.3f
            Box(
                Modifier.size(190.dp)
                    .graphicsLayer {
                        scaleX = scale + extra
                        scaleY = scale + extra
                        alpha = fade
                    }
                    .background(Green, CircleShape)
            )
        }
        Box(
            Modifier.size(190.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF4ADE80), GreenDark)))
                .clickable(onClick = onClick),
            Alignment.Center
        ) {
            if (searching) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(64.dp))
            else Icon(
                if (listening) Icons.Default.Stop else Icons.Default.Mic, null,
                tint = Color.White, modifier = Modifier.size(84.dp)
            )
        }
    }
}

@Composable
fun ResultCard(song: Song, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(song.artwork, 72.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(T("Detaylar ›", "Details ›"), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }
    }
}

// ---------- Ana sayfa ----------

@Composable
fun HomeScreen(vm: MainVM) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val micForListen = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.toggleListen(ctx.applicationContext)
        else Toast.makeText(ctx, T("Mikrofon izni gerekli", "Microphone permission is required"), Toast.LENGTH_LONG).show()
    }
    val micForBubble = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) continueBubble(ctx)
        else Toast.makeText(ctx, T("Mikrofon izni gerekli", "Microphone permission is required"), Toast.LENGTH_LONG).show()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.bubble), null, Modifier.size(40.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Text("SW Müzik Bulucu", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        if (Store.token.isBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth().clickable { vm.tab = 2 },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    T("Başlamak için Ayarlar'dan ücretsiz AudD API anahtarını gir ›",
                        "To start, enter your free AudD API token in Settings ›"),
                    Modifier.padding(14.dp), fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        ListenButton(vm.state, vm.level) {
            if (micOk(ctx)) vm.toggleListen(ctx.applicationContext)
            else micForListen.launch(Manifest.permission.RECORD_AUDIO)
        }
        Text(
            when (vm.state) {
                UiState.Listening -> T("Dinleniyor… (durdurmak için dokun)", "Listening… (tap to stop)")
                UiState.Searching -> T("Şarkı aranıyor…", "Searching…")
                else -> T("Müziği bulmak için dokun", "Tap to identify music")
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))
        when (val s = vm.state) {
            is UiState.Done -> ResultCard(s.song) { vm.detail = s.song }
            UiState.NotFound -> Text(
                T("Şarkı bulunamadı. Müziğe yaklaşıp tekrar dene.", "No match found. Move closer and try again."),
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            is UiState.Failed -> Text(s.msg, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            else -> {}
        }

        Spacer(Modifier.height(20.dp))
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(T("Linkten müzik bul", "Find music from a link"), fontWeight = FontWeight.Bold)
                Text(
                    T("YouTube, Instagram, TikTok vb. video bağlantısı", "YouTube, Instagram, TikTok etc. video link"),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = vm.link, onValueChange = { vm.link = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("https://…") },
                    trailingIcon = {
                        IconButton(onClick = { clip.getText()?.text?.let { vm.link = it } }) {
                            Icon(Icons.Default.ContentPaste, T("Yapıştır", "Paste"))
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.findLink() }, modifier = Modifier.fillMaxWidth(),
                    enabled = vm.link.isNotBlank() && vm.state !is UiState.Searching && vm.state !is UiState.Listening,
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)
                ) { Text(T("Linkten Bul", "Find from link")) }
                Text(
                    T("İpucu: Videoyu uygulamadan Paylaş ▸ SW Müzik Bulucu diyerek de gönderebilirsin.",
                        "Tip: you can also Share a video straight to SW Müzik Bulucu."),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(T("Kayan baloncuk", "Floating bubble"), fontWeight = FontWeight.Bold)
                    Text(
                        T("Diğer uygulamaların üstünde durur, dokununca müziği bulur",
                            "Stays over other apps; tap it to identify music"),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = Store.bubbleOn,
                    onCheckedChange = { on ->
                        if (!on) BubbleService.stop(ctx)
                        else if (!micOk(ctx)) micForBubble.launch(Manifest.permission.RECORD_AUDIO)
                        else continueBubble(ctx)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = Color.White)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------- Geçmiş ----------

@Composable
fun HistoryScreen(vm: MainVM) {
    var confirm by remember { mutableStateOf(false) }
    val list = Store.history
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(T("Geçmiş", "History"), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (list.isNotEmpty()) IconButton(onClick = { confirm = true }) {
                Icon(Icons.Default.DeleteSweep, T("Tümünü sil", "Delete all"))
            }
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(T("Henüz bulunan şarkı yok", "No songs found yet"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { s ->
                    Card(
                        Modifier.fillMaxWidth().clickable { vm.detail = s },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover(s.artwork, 56.dp, 10.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(s.artist, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(fmtDate(s.foundAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { Store.remove(s.id) }) {
                                Icon(Icons.Default.Delete, T("Sil", "Delete"))
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(T("Geçmişi sil?", "Clear history?")) },
        text = { Text(T("Bulunan bütün şarkılar silinecek.", "All found songs will be deleted.")) },
        confirmButton = { TextButton(onClick = { Store.clear(); confirm = false }) { Text(T("Sil", "Delete")) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(T("Vazgeç", "Cancel")) } }
    )
}

// ---------- Şarkı detayı ----------

@Composable
fun DetailScreen(song: Song, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var lyrics by remember(song.id) { mutableStateOf(song.lyrics) }
    var loading by remember(song.id) { mutableStateOf(song.lyrics.isBlank()) }
    LaunchedEffect(song.id) {
        if (lyrics.isBlank()) {
            val l = Finder.lyrics(song)
            if (l.isNotBlank()) {
                lyrics = l
                Store.updateLyrics(song.id, l)
            }
            loading = false
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, T("Geri", "Back")) }
        Box(Modifier.fillMaxWidth(), Alignment.Center) { Cover(song.artwork, 260.dp, 20.dp) }
        Spacer(Modifier.height(16.dp))
        Text(song.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(song.artist, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { openUrl(ctx, song.spotifyUrl) }, Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Spotify", fontSize = 13.sp) }
            FilledTonalButton(onClick = { openUrl(ctx, song.youtubeUrl) }, Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("YouTube", fontSize = 13.sp) }
            FilledTonalButton(onClick = { openUrl(ctx, song.appleUrl) }, Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Apple Music", fontSize = 13.sp) }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(T("Albüm bilgisi", "Album info"), fontWeight = FontWeight.Bold)
                InfoRow(T("Albüm", "Album"), song.album)
                InfoRow(T("Çıkış tarihi", "Release date"), song.releaseDate)
                InfoRow(T("Yayıncı", "Label"), song.label)
                InfoRow(T("Bulunma zamanı", "Found at"), fmtDate(song.foundAt))
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(T("Şarkı sözleri", "Lyrics"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                when {
                    lyrics.isNotBlank() -> Text(lyrics, lineHeight = 22.sp)
                    loading -> CircularProgressIndicator(Modifier.size(24.dp), color = Green)
                    else -> Text(T("Bu şarkı için söz bulunamadı.", "No lyrics found for this song."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.width(120.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, Modifier.weight(1f), fontSize = 14.sp)
    }
}

// ---------- Ayarlar ----------

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun PermRow(title: String, granted: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (granted) Icons.Default.Check else Icons.Default.Close, null,
            tint = if (granted) Green else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(10.dp))
        Text(title, Modifier.weight(1f), fontSize = 14.sp)
        if (granted) Text(T("Verildi", "Granted"), color = Green, fontSize = 13.sp)
        else TextButton(onClick = onClick) { Text(T("İzin ver", "Grant")) }
    }
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val notifL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    fun requestNotif() {
        if (Build.VERSION.SDK_INT >= 33) notifL.launch(Manifest.permission.POST_NOTIFICATIONS)
        else ctx.startActivity(
            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(T("Ayarlar", "Settings"), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))

        SectionCard(T("Müzik tanıma (AudD)", "Music recognition (AudD)")) {
            OutlinedTextField(
                value = Store.token, onValueChange = { Store.setToken(it.trim()) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(T("AudD API anahtarı", "AudD API token")) }
            )
            TextButton(onClick = { openUrl(ctx, "https://dashboard.audd.io") }) {
                Text(T("Ücretsiz anahtar al: dashboard.audd.io", "Get a free token: dashboard.audd.io"))
            }
        }

        SectionCard(T("Tema", "Theme")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to T("Sistem", "System"), "light" to T("Açık", "Light"), "dark" to T("Koyu", "Dark")).forEach { (k, l) ->
                    FilterChip(selected = Store.theme == k, onClick = { Store.setTheme(k) }, label = { Text(l) })
                }
            }
        }

        SectionCard(T("Dil", "Language")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("tr" to "Türkçe", "en" to "English").forEach { (k, l) ->
                    FilterChip(selected = Store.lang == k, onClick = { Store.setLang(k) }, label = { Text(l) })
                }
            }
        }

        SectionCard(T("Bildirimler", "Notifications")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(T("Şarkı bulununca bildirim gönder", "Notify when a song is found"), fontSize = 14.sp)
                    Text(T("Baloncukla bulunan şarkılar için", "For songs found with the bubble"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = Store.notif,
                    onCheckedChange = {
                        Store.setNotif(it)
                        if (it && !notifOk(ctx)) requestNotif()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = Color.White)
                )
            }
        }

        SectionCard(T("İzinler", "Permissions")) {
            val refresh = tick
            PermRow(T("Mikrofon", "Microphone"), micOk(ctx)) { micL.launch(Manifest.permission.RECORD_AUDIO) }
            PermRow(T("Diğer uygulamaların üzerinde göster", "Display over other apps"), AndroidSettings.canDrawOverlays(ctx)) {
                ctx.startActivity(Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            }
            PermRow(T("Bildirimler", "Notifications"), notifOk(ctx)) { requestNotif() }
            OutlinedButton(
                onClick = {
                    ctx.startActivity(
                        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text(T("Uygulama ayarlarını aç", "Open app settings")) }
        }

        SectionCard(T("Hakkında", "About")) {
            Text("SW Müzik Bulucu 1.0", fontSize = 14.sp)
            Text(
                T("Müzik tanıma altyapısı: AudD", "Recognition powered by AudD"),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
