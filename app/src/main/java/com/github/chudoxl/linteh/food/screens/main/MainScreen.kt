package com.github.chudoxl.linteh.food.screens.main

import ThemeDarknessPreviewParamProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.chudoxl.linteh.food.R
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.screens.main.data.StudentState
import com.github.chudoxl.linteh.food.ui.components.InfoButton
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import com.github.chudoxl.linteh.food.utils.launchHlynov
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun MainScreen(
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onAboutClick: () -> Unit = {},
) {
    val viewModel: MainViewModel = viewModel()
    val items by viewModel.students.collectAsState()
    NotificationPermissionEffect(shouldRequest = items.any { it.notificationEnabled })
    MainScreenContent(
        itemList = items,
        onRefreshRequested = { viewModel.refreshBalances() },
        onAddClick = onAddClick,
        onEditClick = onEditClick,
        onDeleteClick = { cardNumber -> viewModel.deleteStudent(cardNumber) },
        onAboutClick = onAboutClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    itemList: ImmutableList<StudentState>,
    onRefreshRequested: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    onAboutClick: () -> Unit = {},
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val state = rememberPullToRefreshState()
    val context = LocalContext.current
    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            isRefreshing = true
            onRefreshRequested()
            delay(1000)
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = Modifier
            .testTag("main_screen"),
        topBar = { TopBar(onAboutClick = onAboutClick) },
        floatingActionButton = {
            if (itemList.isNotEmpty()) FloatingActionButton(
                modifier = Modifier.testTag("fab_add"),
                onClick = onAddClick,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.add_24),
                    contentDescription = "Добавить"
                )
            }
        },
    ) { paddingValues ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("pull_to_refresh"),
        ) {
            if (itemList.isEmpty()) Box(Modifier.fillMaxSize().testTag("empty_state")){
                EmptyWidget(
                    modifier = Modifier.align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    onAddItemClick = onAddClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .testTag("students_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(itemList.size) { idx ->
                        StudentItem(
                            state = itemList[idx],
                            onEditClick = { onEditClick(itemList[idx].cardNumber) },
                            onDeleteClick = { onDeleteClick(itemList[idx].cardNumber) },
                            onTopUpClick = { context.launchHlynov(cardNumber = itemList[idx].cardNumber) },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onAboutClick: () -> Unit = {}) = TopAppBar(
    navigationIcon = {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_fg),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp)
        )
    },
    title = {
        Text(text = stringResource(R.string.app_name))
    },
    actions = {
        InfoButton(
            onClick = onAboutClick,
            modifier = Modifier.testTag("about_button"),
            contentDescription = "О приложении",
        )
    },
)


private val previewListItems = persistentListOf(
    StudentState(
        student = Student(
            name = "Анна",
            cardNumber = "123456789",
            balance = BigDecimal("10688.89"),
            balanceState = BalanceState.Loaded,
            password = "xxx",
            notificationEnabled = true,
            notificationTime = kotlinx.datetime.LocalTime(hour = 7, minute = 0),
            notificationThreshold = BigDecimal(200)
        )
    ),
    StudentState(
        student = Student(
            name = "Петрович",
            cardNumber = "321000123",
            balance = BigDecimal("150.39"),
            balanceState = BalanceState.Error(),
            password = "xxx",
            notificationEnabled = true,
            notificationTime = kotlinx.datetime.LocalTime(hour = 7, minute = 0),
            notificationThreshold = BigDecimal(200)
        )
    ),
    StudentState(
        student = Student(
            name = "Василий",
            cardNumber = "987654321",
            notificationEnabled = false,
            balance = BigDecimal.ZERO,
            balanceState = BalanceState.Refreshing,
            password = "xxx",
            notificationTime = kotlinx.datetime.LocalTime(hour = 7, minute = 0),
            notificationThreshold = BigDecimal(200),

        )
    )
)

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun MainScreenPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme = darkTheme) {
        MainScreenContent(previewListItems)
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun MainScreenEmptyPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme = darkTheme) {
        MainScreenContent(persistentListOf())
    }
}
