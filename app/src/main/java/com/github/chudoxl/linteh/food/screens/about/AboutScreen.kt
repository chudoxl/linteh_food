package com.github.chudoxl.linteh.food.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.chudoxl.linteh.food.R
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onLicensesClick: () -> Unit,
) {
    val viewModel: AboutViewModel = viewModel()
    val markdownState by viewModel.markdownState.collectAsState()
    Scaffold(
        modifier = Modifier.testTag("about_screen"),
        topBar = {
            TopAppBar(
                title = { Text("О приложении") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("about_back")) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.arrow_back_24),
                            contentDescription = "Назад",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Markdown(
                state = markdownState,
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.headlineSmall,
                    h2 = MaterialTheme.typography.titleLarge,
                    h3 = MaterialTheme.typography.titleMedium,
                    h4 = MaterialTheme.typography.titleSmall,
                    h5 = MaterialTheme.typography.titleSmall,
                    h6 = MaterialTheme.typography.titleSmall,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            TextButton(
                onClick = onLicensesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("licenses_button"),
            ) {
                Text(
                    text = "Лицензии",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.chevron_right_24),
                    contentDescription = null,
                )
            }
        }
    }
}
