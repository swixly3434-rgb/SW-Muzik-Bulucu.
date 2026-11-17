package com.sw.muzikbulucu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Idle : UiState
    data object Listening : UiState
    data object Searching : UiState
    data class Done(val song: Song) : UiState
    data object NotFound : UiState
    data class Failed(val msg: String) : UiState
}

class MainVM : ViewModel() {
    var state by mutableStateOf<UiState>(UiState.Idle)
    var level by mutableIntStateOf(0)
    var link by mutableStateOf("")
    var detail by mutableStateOf<Song?>(null)
    var tab by mutableIntStateOf(0)
    private var job: Job? = null

    private fun busy() = state is UiState.Listening || state is UiState.Searching

    fun toggleListen(ctx: Context) {
        if (state is UiState.Listening) {
            job?.cancel()
            state = UiState.Idle
            return
        }
        if (busy()) return
        state = UiState.Listening
        job = viewModelScope.launch {
            val out = Finder.listen(ctx, 10, onLevel = { level = it }, onRecorded = { state = UiState.Searching })
            finish(out)
        }
    }

    fun findLink() {
        val url = Finder.extractUrl(link)
        if (url == null) {
            state = UiState.Failed(T("Geçerli bir bağlantı yapıştır (https://...)", "Paste a valid link (https://...)"))
            return
        }
        if (busy()) return
        state = UiState.Searching
        job = viewModelScope.launch { finish(Finder.fromLink(url)) }
    }

    private fun finish(out: Outcome) {
        state = when (out) {
            is Outcome.Found -> {
                Store.add(out.song)
                UiState.Done(out.song)
            }
            Outcome.NotFound -> UiState.NotFound
            is Outcome.Error -> UiState.Failed(out.msg)
        }
    }
}

class MainActivity : ComponentActivity() {
    private val vm: MainVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot(vm) }
        if (savedInstanceState == null) handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        i ?: return
        i.getStringExtra("song_id")?.let { id ->
            Store.find(id)?.let { vm.detail = it }
        }
        if (i.action == Intent.ACTION_SEND && i.type == "text/plain") {
            val text = i.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val url = Finder.extractUrl(text)
            if (url != null) {
                vm.tab = 0
                vm.detail = null
                vm.link = url
                vm.findLink()
            }
        }
    }
}
