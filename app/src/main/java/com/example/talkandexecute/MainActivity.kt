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
    private val gptViewModel: ChatGPTViewModel by viewModels()
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
                    TalkComposable(gptViewModel, audioManager)
                }
            }
        }

        // Call the method to copy specific file types from assets to data folder
        // Use it once!
        // val extensionsToCopy = arrayOf("bin")
        // copyAssetsWithExtensionsToDataFolder(this, extensionsToCopy)
    }

    // Copy assets to data folder
    private fun copyAssetsWithExtensionsToDataFolder(context: Context, extensions: Array<String>) {
        val assetManager = context.assets
        try {
            // Specify the destination directory in the app's data folder
            val destFolder = context.filesDir.absolutePath
            for (extension in extensions) {
                // List all files in the assets folder with the specified extension
                val assetFiles = assetManager.list("")
                for (assetFileName in assetFiles!!) {
                    if (assetFileName.endsWith(".$extension")) {
                        val outFile = File(destFolder, assetFileName)
                        if (outFile.exists()) continue
                        val inputStream = assetManager.open(assetFileName)
                        val outputStream: OutputStream = FileOutputStream(outFile)

                        // Copy the file from assets to the data folder
                        val buffer = ByteArray(1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        inputStream.close()
                        outputStream.flush()
                        outputStream.close()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
