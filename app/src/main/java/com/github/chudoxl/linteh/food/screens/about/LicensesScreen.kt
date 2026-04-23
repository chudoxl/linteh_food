package com.github.chudoxl.linteh.food.screens.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import com.github.chudoxl.linteh.food.R
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val resources = LocalResources.current
    val libraries by produceLibraries {
        withContext(Dispatchers.IO) {
            resources.openRawResource(R.raw.aboutlibraries)
                .bufferedReader()
                .use { it.readText() }
        }
    }
    Scaffold(
        modifier = Modifier.testTag("licenses_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Лицензии") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("licenses_back")) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.arrow_back_24),
                            contentDescription = "Назад",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}
