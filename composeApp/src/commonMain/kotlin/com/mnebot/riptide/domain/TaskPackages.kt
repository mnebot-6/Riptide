package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.PackageTask
import com.mnebot.riptide.domain.model.Recurrence
import com.mnebot.riptide.domain.model.TaskPackage
import com.mnebot.riptide.domain.model.WeeklySlot
import kotlinx.datetime.LocalTime
import riptide.composeapp.generated.resources.*

/** Six predefined task packages that users can install from Settings or during onboarding. */
object TaskPackages {

    private val DAILY_ALL_WEEK = Recurrence.Weekly(
        (1..7).map { WeeklySlot(dayOfWeek = it, startTime = null, endTime = null) }
    )

    private val WEEKDAYS = Recurrence.Weekly(
        (1..5).map { WeeklySlot(dayOfWeek = it, startTime = null, endTime = null) }
    )

    private val WEEKLY_ONCE = Recurrence.Weekly(
        listOf(WeeklySlot(dayOfWeek = 1, startTime = null, endTime = null))
    )

    val HEALTH = TaskPackage(
        id = "pkg_health",
        nameRes = Res.string.pkg_health_name,
        descriptionRes = Res.string.pkg_health_desc,
        icon = "\uD83D\uDC9A",
        color = "#34A853",
        tasks = listOf(
            PackageTask(
                titleRes = Res.string.task_drink_water,
                recurrence = DAILY_ALL_WEEK,
                targetCount = 8
            ),
            PackageTask(
                titleRes = Res.string.task_vitamins,
                time = LocalTime(9, 0),
                recurrence = DAILY_ALL_WEEK
            ),
            PackageTask(
                titleRes = Res.string.task_meditate_10,
                time = LocalTime(7, 30),
                recurrence = DAILY_ALL_WEEK,
                timerDurationMinutes = 10
            ),
            PackageTask(
                titleRes = Res.string.task_health_checkup,
                recurrence = WEEKLY_ONCE
            ),
            PackageTask(
                titleRes = Res.string.task_sleep_8h,
                time = LocalTime(23, 0),
                recurrence = DAILY_ALL_WEEK,
                isPriority = true
            )
        )
    )

    val FITNESS = TaskPackage(
        id = "pkg_fitness",
        nameRes = Res.string.pkg_fitness_name,
        descriptionRes = Res.string.pkg_fitness_desc,
        icon = "\uD83D\uDCAA",
        color = "#FF6B35",
        tasks = listOf(
            PackageTask(
                titleRes = Res.string.task_exercise_30,
                recurrence = WEEKDAYS,
                timerDurationMinutes = 30,
                isPriority = true
            ),
            PackageTask(
                titleRes = Res.string.task_morning_stretch,
                time = LocalTime(7, 0),
                recurrence = DAILY_ALL_WEEK,
                timerDurationMinutes = 10
            ),
            PackageTask(
                titleRes = Res.string.task_cardio,
                recurrence = Recurrence.Weekly(
                    listOf(
                        WeeklySlot(dayOfWeek = 2, startTime = null, endTime = null),
                        WeeklySlot(dayOfWeek = 4, startTime = null, endTime = null),
                        WeeklySlot(dayOfWeek = 6, startTime = null, endTime = null)
                    )
                ),
                timerDurationMinutes = 20
            ),
            PackageTask(
                titleRes = Res.string.task_leg_day,
                recurrence = Recurrence.Weekly(
                    listOf(
                        WeeklySlot(dayOfWeek = 1, startTime = null, endTime = null),
                        WeeklySlot(dayOfWeek = 3, startTime = null, endTime = null)
                    )
                )
            ),
            PackageTask(
                titleRes = Res.string.task_rest_day,
                recurrence = Recurrence.Weekly(
                    listOf(WeeklySlot(dayOfWeek = 7, startTime = null, endTime = null))
                )
            )
        )
    )

    val STUDY = TaskPackage(
        id = "pkg_study",
        nameRes = Res.string.pkg_study_name,
        descriptionRes = Res.string.pkg_study_desc,
        icon = "\uD83D\uDCDA",
        color = "#F4A300",
        tasks = listOf(
            PackageTask(
                titleRes = Res.string.task_study_2h,
                recurrence = WEEKDAYS,
                timerDurationMinutes = 120,
                isPriority = true
            ),
            PackageTask(
                titleRes = Res.string.task_review_notes,
                recurrence = WEEKDAYS
            ),
            PackageTask(
                titleRes = Res.string.task_practice,
                recurrence = WEEKDAYS
            ),
            PackageTask(
                titleRes = Res.string.task_read_30,
                time = LocalTime(21, 0),
                recurrence = DAILY_ALL_WEEK,
                timerDurationMinutes = 30
            )
        )
    )

    val MINDFULNESS = TaskPackage(
        id = "pkg_mindfulness",
        nameRes = Res.string.pkg_mindfulness_name,
        descriptionRes = Res.string.pkg_mindfulness_desc,
        icon = "\uD83E\uDDD8",
        color = "#9C27B0",
        tasks = listOf(
            PackageTask(
                titleRes = Res.string.task_meditation,
                time = LocalTime(7, 0),
                recurrence = DAILY_ALL_WEEK,
                timerDurationMinutes = 15,
                isPriority = true
            ),
            PackageTask(
                titleRes = Res.string.task_gratitude,
                time = LocalTime(22, 0),
                recurrence = DAILY_ALL_WEEK
            ),
            PackageTask(
                titleRes = Res.string.task_breathing,
                recurrence = DAILY_ALL_WEEK,
                timerDurationMinutes = 5
            ),
            PackageTask(
                titleRes = Res.string.task_digital_detox,
                recurrence = DAILY_ALL_WEEK,
                timerDurationMinutes = 60
            )
        )
    )

    val HOME = TaskPackage(
        id = "pkg_home",
        nameRes = Res.string.pkg_home_name,
        descriptionRes = Res.string.pkg_home_desc,
        icon = "\uD83C\uDFE0",
        color = "#795548",
        tasks = listOf(
            PackageTask(
                titleRes = Res.string.task_clean_kitchen,
                recurrence = DAILY_ALL_WEEK
            ),
            PackageTask(
                titleRes = Res.string.task_laundry,
                recurrence = Recurrence.Weekly(
                    listOf(
                        WeeklySlot(dayOfWeek = 3, startTime = null, endTime = null),
                        WeeklySlot(dayOfWeek = 6, startTime = null, endTime = null)
                    )
                )
            ),
            PackageTask(
                titleRes = Res.string.task_groceries,
                recurrence = WEEKLY_ONCE
            ),
            PackageTask(
                titleRes = Res.string.task_plants,
                recurrence = Recurrence.Weekly(
                    listOf(
                        WeeklySlot(dayOfWeek = 1, startTime = null, endTime = null),
                        WeeklySlot(dayOfWeek = 4, startTime = null, endTime = null)
                    )
                )
            )
        )
    )

    val FINANCE = TaskPackage(
        id = "pkg_finance",
        nameRes = Res.string.pkg_finance_name,
        descriptionRes = Res.string.pkg_finance_desc,
        icon = "\uD83D\uDCB0",
        color = "#607D8B",
        tasks = listOf(
            PackageTask(
                titleRes = Res.string.task_review_expenses,
                recurrence = DAILY_ALL_WEEK
            ),
            PackageTask(
                titleRes = Res.string.task_weekly_budget,
                recurrence = WEEKLY_ONCE,
                isPriority = true
            ),
            PackageTask(
                titleRes = Res.string.task_savings,
                recurrence = Recurrence.Weekly(
                    listOf(WeeklySlot(dayOfWeek = 1, startTime = null, endTime = null))
                )
            ),
            PackageTask(
                titleRes = Res.string.task_bills,
                recurrence = Recurrence.Weekly(
                    listOf(WeeklySlot(dayOfWeek = 1, startTime = null, endTime = null))
                )
            )
        )
    )

    /** All available packages in display order. */
    val ALL = listOf(HEALTH, FITNESS, STUDY, MINDFULNESS, HOME, FINANCE)

    fun findById(id: String): TaskPackage? = ALL.find { it.id == id }
}
