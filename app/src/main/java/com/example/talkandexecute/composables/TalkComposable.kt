package com.example.talkandexecute.composables

import android.Manifest
import android.media.AudioManager
import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.talkandexecute.ChatGPTViewModel
import com.example.talkandexecute.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TalkComposable(
    viewModel: ChatGPTViewModel,
    audioManager: AudioManager,
    modifier: Modifier = Modifier
) {

    // Based on the result adjust the volume.
    if (viewModel.speechState.llmResult.contains("up")) {
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    } else if (viewModel.speechState.llmResult.contains("down")) {
        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }

    // Check for permissions.
    val permissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.RECORD_AUDIO
        )
    )
    when {
        permissions.allPermissionsGranted -> {
            // viewModel.someFunction()
        }

        else -> LaunchedEffect(Unit) { permissions.launchMultiplePermissionRequest() }
    }

    // Change the border color based on the theme
    val borderColor =
        if (isSystemInDarkTheme()) Color.White else Color.Black

    Column(
        modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Click the button and talk",
            modifier = modifier
                .align(Alignment.CenterHorizontally)
                // .border(1.dp, borderColor, shape = RoundedCornerShape(4.dp))
                .padding(8.dp)
        )

        Spacer(modifier = modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.baseline_mic_36),
            contentDescription = null,
            modifier = modifier
                .size(140.dp)
                .clip(CircleShape)
                .clickable { /* Do something */ }
                .pointerInteropFilter {
                    when (it.action) {
                        MotionEvent.ACTION_UP -> {
                            // viewModel.stopListening()
                            // or for offline transcription
                            viewModel.stopRecordingWav()
                            viewModel.speechState = viewModel.speechState.copy(speechResult = "")
                        }

                        else -> {
                            // viewModel.startListening()
                            // or for offline transcription
                            viewModel.startRecordingWav()
                            viewModel.speechState = viewModel.speechState.copy(speechResult = "")
                            viewModel.speechState = viewModel.speechState.copy(llmResult = "")
                        }
                    }
                    true
                },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(colorResource(id = R.color.teal_700))
        )

        Spacer(modifier = modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = viewModel.speechState.speechResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .border(1.dp, borderColor, shape = RoundedCornerShape(4.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        Spacer(modifier = modifier.height(16.dp))

        Text(
            text = "Result from LLMs",
            modifier = modifier
                .align(Alignment.CenterHorizontally)
                // .border(1.dp, borderColor, shape = RoundedCornerShape(4.dp))
                .padding(8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Button for ChatGPT
            Text(
                text = "ChatGPT",
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp, horizontal = 16.dp)
                    .height(32.dp)
                    .border(1.dp, borderColor, shape = RoundedCornerShape(4.dp))
                    .clickable {
                        viewModel.pickAPI(false)
                        viewModel.speechState = viewModel.speechState.copy(llmResult = "")
                    }
                    .wrapContentSize(Alignment.Center),
                color = Color.White,
                fontSize = 16.sp
            )

            Spacer(modifier = modifier.width(16.dp))

            // Button for Gemini
            Text(
                text = "Gemini",
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp, horizontal = 16.dp)
                    .height(32.dp)
                    .border(1.dp, borderColor, shape = RoundedCornerShape(4.dp))
                    .clickable {
                        viewModel.pickAPI(true)
                        viewModel.speechState = viewModel.speechState.copy(llmResult = "")
                    }
                    .wrapContentSize(Alignment.Center),
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = viewModel.speechState.llmResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .border(1.dp, borderColor, shape = RoundedCornerShape(4.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }
    }
}
