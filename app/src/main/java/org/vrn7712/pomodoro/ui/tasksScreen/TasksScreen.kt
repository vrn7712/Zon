package org.vrn7712.pomodoro.ui.tasksScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.vrn7712.pomodoro.R
import org.vrn7712.pomodoro.data.Task
import org.vrn7712.pomodoro.ui.tasksScreen.viewModel.TasksViewModel
import org.vrn7712.pomodoro.ui.theme.CustomColors.topBarColors
import org.vrn7712.pomodoro.ui.theme.AppFonts.robotoFlexTopBar
import org.vrn7712.pomodoro.ui.theme.AppFonts

import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreenRoot(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory)
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    var showConfetti by remember { mutableStateOf(false) }
    var previousCompleted by remember { mutableIntStateOf(stats.completed) }

    LaunchedEffect(stats) {
        if (stats.total > 0 && stats.completed == stats.total && stats.completed > previousCompleted) {
            showConfetti = true
            kotlinx.coroutines.delay(3000)
            showConfetti = false
        }
        previousCompleted = stats.completed
    }

    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.tasks),
                            style = LocalTextStyle.current.copy(
                                fontFamily = robotoFlexTopBar,
                                fontSize = 32.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    subtitle = {},
                    colors = topBarColors,
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                LargeFloatingActionButton(
                    onClick = { 
                        taskToEdit = null
                        showAddDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
                ) {
                    Icon(painterResource(R.drawable.add), contentDescription = stringResource(R.string.add_task), modifier = Modifier.size(36.dp))
                }
            },
        ) { innerPadding ->
            val insets = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 88.dp
            )
    
            var selectedFilter by remember { mutableStateOf("All") }
            val filters = listOf("All", "Pending", "Completed", "High Priority")
    
            val filteredTasks = remember(tasks, selectedFilter) {
                when (selectedFilter) {
                    "Pending" -> tasks.filter { !it.isCompleted }
                    "Completed" -> tasks.filter { it.isCompleted }
                    "High Priority" -> tasks.filter { it.priority == 3 }
                    else -> tasks
                }
            }
    
            LazyColumn(
                contentPadding = insets,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(topBarColors.containerColor)
                    .padding(horizontal = 16.dp)
            ) {
                item { 
                    StatisticsCard(
                        stats = stats,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
    
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filters.forEach { filter ->
                            FilterChip(
                                selected = filter == selectedFilter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                leadingIcon = if (filter == selectedFilter) {
                                    { Icon(painterResource(R.drawable.check), null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
    
                if (filteredTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                                .animateItem(), // Animate entry/exit using the new API
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.view_day),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (tasks.isEmpty()) stringResource(R.string.no_tasks_yet) else "No tasks found",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteTask(task)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Task deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreTask(task)
                                    }
                                }
                            }
                        }
    
                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.animateItem(), // Magic happens here with new API
                            backgroundContent = {
                                val color = MaterialTheme.colorScheme.errorContainer
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.extraLarge)
                                        .background(color)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = stringResource(R.string.delete_task),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            TaskItem(
                                task = task,
                                onToggle = { viewModel.toggleTask(task) },
                                onClick = {
                                    taskToEdit = task
                                    showAddDialog = true
                                }
                            )
                        }
                    }
                    
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
    
            if (showAddDialog) {
                ModalBottomSheet(
                    onDismissRequest = { showAddDialog = false }
                ) {
                    AddTaskSheet(
                        taskToEdit = taskToEdit,
                        onDismiss = { showAddDialog = false },
                        onConfirm = { title, subject, dueDate, notes, priority ->
                            if (taskToEdit == null) {
                                viewModel.addTask(title, subject, dueDate, notes, priority)
                            } else {
                                viewModel.updateTask(taskToEdit!!.copy(
                                    title = title,
                                    subject = subject,
                                    dueDate = dueDate,
                                    notes = notes,
                                    priority = priority
                                ))
                            }
                            showAddDialog = false
                        }
                    )
                }
            }
        }
        
        org.vrn7712.pomodoro.ui.components.Confetti(isExploding = showConfetti)
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (task.isCompleted) 
                MaterialTheme.colorScheme.surfaceContainerHighest // Solid color, no alpha
            else 
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (task.isCompleted) 0.dp else 4.dp, // Flat when completed
            pressedElevation = 2.dp
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Priority Indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(
                        when (task.priority) {
                            3 -> MaterialTheme.colorScheme.error // High
                            2 -> MaterialTheme.colorScheme.tertiary // Medium
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // Low
                        }
                    )
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .weight(1f)
            ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(1.3f)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.googleFlex600,
                        fontWeight = FontWeight.ExtraBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                )
                if (task.subject != "General" || task.dueDate != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.subject != "General") {
                           val icon = getSubjectIcon(task.subject)
                           Surface(
                               shape = MaterialTheme.shapes.small,
                               color = MaterialTheme.colorScheme.primaryContainer,
                               modifier = Modifier.height(28.dp)
                           ) {
                               Row(
                                   verticalAlignment = Alignment.CenterVertically,
                                   modifier = Modifier.padding(horizontal = 12.dp)
                               ) {
                                   Icon(
                                       painter = painterResource(icon),
                                       contentDescription = null,
                                       modifier = Modifier.size(16.dp),
                                       tint = MaterialTheme.colorScheme.onPrimaryContainer
                                   )
                                   Spacer(modifier = Modifier.width(6.dp))
                                   Text(
                                       text = task.subject,
                                       style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                       color = MaterialTheme.colorScheme.onPrimaryContainer
                                   )
                               }
                           }
                           Spacer(modifier = Modifier.width(12.dp))
                        }
                        if (task.dueDate != null) {
                           Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                   painter = painterResource(R.drawable.calendar_today),
                                   contentDescription = null,
                                   modifier = Modifier.size(18.dp),
                                   tint = MaterialTheme.colorScheme.outline
                               )
                               Spacer(modifier = Modifier.width(6.dp))
                               val dateString = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(task.dueDate))
                               Text(
                                   text = dateString,
                                   style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                   color = MaterialTheme.colorScheme.outline
                               )
                           }
                        }
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?, String?, Int) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var notes by remember { mutableStateOf(taskToEdit?.notes ?: "") }
    var selectedSubject by remember { mutableStateOf(taskToEdit?.subject ?: "General") }
    var priority by remember { mutableIntStateOf(taskToEdit?.priority ?: 2) } 
    var dueDate by remember { mutableStateOf(taskToEdit?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val subjects = listOf("General", "Mathematics", "Physics", "Chemistry", "Biology", "History", "Geography", "Computing", "Literature")
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (taskToEdit == null) "Create Task" else "Edit Task",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = AppFonts.googleFlex600,
                    fontWeight = FontWeight.Bold
                )
            )
            IconButton(onClick = onDismiss) {
                Icon(painterResource(R.drawable.close), contentDescription = "Close")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("What are you studying?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("e.g. Calculus Chapter 4") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            singleLine = true,
            trailingIcon = { Icon(painterResource(R.drawable.edit), null) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Select Subject", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEach { subject ->
                FilterChip(
                    selected = subject == selectedSubject,
                    onClick = { selectedSubject = subject },
                    label = { Text(subject) },
                    leadingIcon = {
                        Icon(painterResource(getSubjectIcon(subject)), null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Priority", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Low" to 1, "Medium" to 2, "High" to 3).forEach { (label, value) ->
                FilterChip(
                    selected = priority == value,
                    onClick = { priority = value },
                    label = { Text(label) },
                    leadingIcon = if (priority == value) {
                        { Icon(painterResource(R.drawable.check), null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when(value) {
                            3 -> MaterialTheme.colorScheme.errorContainer
                            2 -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        selectedLabelColor = when(value) {
                            3 -> MaterialTheme.colorScheme.onErrorContainer
                            2 -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Due Date (Optional)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.calendar_today), null)
                Spacer(modifier = Modifier.width(12.dp))
                if (dueDate != null) {
                    Text(
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(dueDate!!)),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                } else {
                    Text("mm/dd/yyyy", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Notes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = { Text("Add details about formulas...") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = MaterialTheme.shapes.extraLarge
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        SwipeToSaveButton(
            isSaving = isSaving,
            enabled = title.isNotBlank(),
            onSwipeComplete = {
                isSaving = true
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onConfirm(title, selectedSubject, dueDate, notes.ifBlank { null }, priority)
                    isSaving = false
                }, 1500)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SwipeToSaveButton(
    isSaving: Boolean,
    enabled: Boolean,
    onSwipeComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val width = 300.dp
    val dragSize = 56.dp
    
    // Simple state tracking for swipe
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipe = with(LocalDensity.current) { (width - dragSize - 8.dp).toPx() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (isSaving) {
           Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
               CircularProgressIndicator(
                   color = MaterialTheme.colorScheme.primary, // Used primary color for high visibility
                   modifier = Modifier.size(32.dp),
                   strokeWidth = 3.dp
               )
           } 
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Slide to save task",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.toInt(), 0) }
                    .padding(4.dp)
                    .size(dragSize)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                           if (enabled && !isSaving) {
                               val newOffset = (offsetX + delta).coerceIn(0f, maxSwipe)
                               offsetX = newOffset
                           }
                        },
                        onDragStopped = {
                             if (offsetX > maxSwipe * 0.9f) {
                                 onSwipeComplete()
                                 haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                 offsetX = 0f // Reset
                             } else {
                                 // Snap back
                                 offsetX = 0f
                             }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.chevron_right), 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onPrimary
                )
             }
        }
    }
}

fun getSubjectIcon(subject: String): Int {
    return when(subject) {
        "Mathematics" -> R.drawable.subject_math
        "Physics" -> R.drawable.subject_physics
        "Chemistry" -> R.drawable.subject_chemistry
        "Biology" -> R.drawable.subject_biology
        "History" -> R.drawable.subject_history
        "Geography" -> R.drawable.subject_geography
        "Computing" -> R.drawable.subject_computing
        "Literature" -> R.drawable.subject_literature
        else -> R.drawable.subject_general
    }
}
