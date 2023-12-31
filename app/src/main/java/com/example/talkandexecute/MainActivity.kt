package com.example.talkandexecute

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.talkandexecute.composables.TalkComposable
import com.example.talkandexecute.ui.theme.TalkAndExecuteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TalkAndExecuteViewModel by viewModels()
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager =  getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setContent {
            TalkAndExecuteTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TalkComposable(viewModel, audioManager)
                }
            }
        }
    }
}
