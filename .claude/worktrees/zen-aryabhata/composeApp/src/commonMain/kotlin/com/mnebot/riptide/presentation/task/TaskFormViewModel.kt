// presentation/task/TaskFormViewModel.kt
package com.mnebot.riptide.presentation.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.RecurringTaskDefRepository
import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.presentation.main.currentDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

sealed class TaskFormResult {
    object Saved : TaskFormResult()
    object Deleted : TaskFormResult()
}

class TaskFormViewModel(
    private val dayTaskRepository: DayTaskRepository,
    private val recurringTaskDefRepository: RecurringTaskDefRepository,
    private val existingTaskId: String? = null,
    private val existingRecurringDefId: String? = null
) : ViewModel() {

    private val _result = MutableStateFlow<TaskFormResult?>(null)
    val result: StateFlow<TaskFormResult?> = _result.asStateFlow()

    // Guarda tarea puntual
    fun saveOneTimeTask(
        title: String,
        blockId: String?,
        date: LocalDate,
        time: LocalTime?
    ) {
        viewModelScope.launch {
            val task = DayTask(
                id = existingTaskId ?: generateUUID(),
                blockId = blockId,
                title = title,
                schedule = TaskSchedule.OneTime(date = date, time = time),
                status = TaskStatus.PENDING,
                completedAt = null,
                postponedTo = null,
                sourceTaskId = null
            )
            if (existingTaskId != null) {
                dayTaskRepository.update(task)
            } else {
                dayTaskRepository.insert(task)
            }
            _result.value = TaskFormResult.Saved
        }
    }

    // Guarda tarea recurrente (definición + genera instancias futuras)
    fun saveRecurringTask(
        title: String,
        blockId: String,
        time: LocalTime,
        recurrence: Recurrence
    ) {
        viewModelScope.launch {
            val defId = existingRecurringDefId ?: generateUUID()
            val def = RecurringTaskDef(
                id = defId,
                blockId = blockId,
                title = title,
                time = time,
                recurrence = recurrence,
                isActive = true
            )
            if (existingRecurringDefId != null) {
                recurringTaskDefRepository.update(def)
                // Borra instancias futuras pendientes para regenerarlas
                val today = currentDate()
                dayTaskRepository.getBySourceTask(defId)
                    .filter { it.status == TaskStatus.PENDING }
                    .filter {
                        val schedule = it.schedule
                        schedule is TaskSchedule.OneTime && schedule.date >= today
                    }
                    .forEach { dayTaskRepository.delete(it.id) }
            } else {
                recurringTaskDefRepository.insert(def)
            }
            _result.value = TaskFormResult.Saved
        }
    }

    fun deleteOneTimeTask(id: String) {
        viewModelScope.launch {
            dayTaskRepository.delete(id)
            _result.value = TaskFormResult.Deleted
        }
    }

    fun deleteRecurringDef(id: String) {
        viewModelScope.launch {
            // Borra instancias pendientes futuras, conserva historial
            val today = currentDate()
            dayTaskRepository.getBySourceTask(id)
                .filter { it.status == TaskStatus.PENDING }
                .filter {
                    val schedule = it.schedule
                    schedule is TaskSchedule.OneTime && schedule.date >= today
                }
                .forEach { dayTaskRepository.delete(it.id) }
            recurringTaskDefRepository.delete(id)
            _result.value = TaskFormResult.Deleted
        }
    }

    fun postponeTask(task: DayTask, postponedTo: LocalDateTime) {
        viewModelScope.launch {
            dayTaskRepository.update(
                task.copy(
                    status = TaskStatus.POSTPONED,
                    postponedTo = postponedTo
                )
            )
            // Crea nueva instancia en la fecha/hora pospuesta
            dayTaskRepository.insert(
                task.copy(
                    id = generateUUID(),
                    schedule = TaskSchedule.OneTime(
                        date = postponedTo.date,
                        time = postponedTo.time
                    ),
                    status = TaskStatus.PENDING,
                    postponedTo = null
                )
            )
            _result.value = TaskFormResult.Saved
        }
    }
}