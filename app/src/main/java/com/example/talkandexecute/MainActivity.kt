/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : ComponentActivity() {

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
