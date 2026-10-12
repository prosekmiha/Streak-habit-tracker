package com.streak.app.tiles

import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.widget.WidgetHabit
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickTilesTest {

    private fun item(goal: GoalType, state: DayState = DayState.PENDING) =
        WidgetHabit(testHabit("x", goalType = goal, target = 8.0), state, 0.0, 0, 0, StreakUnit.DAY)

    @Test fun `habit tile logs yes-no and quantity, opens the app otherwise`() {
        assertEquals(TileClick.CONFIGURE, QuickTiles.habitClick(null))
        assertEquals(TileClick.LOG, QuickTiles.habitClick(item(GoalType.YES_NO)))
        assertEquals(TileClick.LOG, QuickTiles.habitClick(item(GoalType.YES_NO, DayState.DONE)))
        assertEquals(TileClick.LOG, QuickTiles.habitClick(item(GoalType.QUANTITY)))
        // Casovna navada in opuscanje nista izbirni za ploscico navade
        assertEquals(TileClick.CONFIGURE, QuickTiles.habitClick(item(GoalType.DURATION)))
        assertEquals(TileClick.CONFIGURE, QuickTiles.habitClick(item(GoalType.QUIT)))
    }

    @Test fun `session tile only accepts timed habits`() {
        assertTrue(QuickTiles.supportsSessionTile(testHabit(goalType = GoalType.DURATION)))
        assertFalse(QuickTiles.supportsSessionTile(testHabit(goalType = GoalType.YES_NO)))
        assertFalse(QuickTiles.supportsHabitTile(testHabit(goalType = GoalType.QUIT)))
    }
}
