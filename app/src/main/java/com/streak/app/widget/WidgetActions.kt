package com.streak.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.ToggleHabitUseCase
import com.streak.app.domain.widget.WidgetSummary
import com.streak.app.domain.widget.WidgetTap
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// Vpisi z widgeta (vedno za danasnji logicni dan)
@Singleton
class WidgetActions @Inject constructor(
    private val habitRepository: HabitRepository,
    private val logHabit: ToggleHabitUseCase,
    private val dayClock: DayClock,
    private val updater: WidgetUpdater
) {
    // Enako kot dotik kartice: opravi/odznaci ali +1 pri kolicini
    suspend fun tap(habitId: String) {
        val item = habitRepository.observeHabits().first().firstOrNull { it.habit.id == habitId } ?: return
        when (WidgetSummary.habit(item).tap) {
            WidgetTap.TOGGLE -> logHabit(item.habit, dayClock.today())
            WidgetTap.ADD_ONE -> logHabit.addProgress(item.habit, dayClock.today(), 1.0)
            WidgetTap.OPEN_SESSION -> return
        }
        updater.updateAll()
    }

    suspend fun addProgress(habitId: String, delta: Double) {
        val habit = habitRepository.getHabit(habitId)?.takeIf { it.goalType == GoalType.QUANTITY && !it.isArchived } ?: return
        logHabit.addProgress(habit, dayClock.today(), delta)
        updater.updateAll()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetRepository(): WidgetRepository
    fun widgetActions(): WidgetActions
}

fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

object WidgetParams {
    val HabitId = ActionParameters.Key<String>("habit_id")
    val Delta = ActionParameters.Key<Double>("delta")
}

class TapHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetParams.HabitId] ?: return
        widgetEntryPoint(context).widgetActions().tap(id)
    }
}

class AddProgressAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetParams.HabitId] ?: return
        val delta = parameters[WidgetParams.Delta] ?: 1.0
        widgetEntryPoint(context).widgetActions().addProgress(id, delta)
    }
}
