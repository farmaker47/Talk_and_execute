package com.example.talkandexecute.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.talkandexecute.R
import com.example.talkandexecute.TalkAndExecuteViewModel

@Composable
fun TalkComposable(viewModel: TalkAndExecuteViewModel, modifier: Modifier = Modifier) {

    val borderColor =
        if (isSystemInDarkTheme()) Color.White else Color.Black // Change the border color based on the theme

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
                .border(1.dp, borderColor, shape = RoundedCornerShape(4.dp))
                .padding(8.dp)
        )

        Spacer(modifier = modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.baseline_mic_36),
            contentDescription = null,
            modifier = modifier
                .size(160.dp)
                .clip(CircleShape)
                .clickable { /* Do something */ },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(colorResource(id = R.color.teal_700))
        )

        Spacer(modifier = modifier.height(16.dp))

        Text(
            text = "Result text",
            modifier = modifier
                .align(Alignment.CenterHorizontally)
                .border(1.dp, borderColor, shape = RoundedCornerShape(4.dp))
                .padding(8.dp)
        )
    }
}
