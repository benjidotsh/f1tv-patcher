package dev.benji.f1tvpatcher

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WeeklyUpdateSchedulerTest {
    private val zone = ZoneId.of("Europe/Brussels")

    @Test
    fun schedulesUpcomingFridayMidnight() {
        val now = ZonedDateTime.of(2026, 5, 13, 12, 0, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 5, 15, 0, 0, 0, 0, zone)

        assertEquals(
            expected.toInstant().toEpochMilli() - now.toInstant().toEpochMilli(),
            WeeklyUpdateScheduler.delayUntilNextFridayMidnight(now),
        )
    }

    @Test
    fun movesToNextWeekWhenFridayMidnightAlreadyPassed() {
        val now = ZonedDateTime.of(2026, 5, 15, 1, 0, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 5, 22, 0, 0, 0, 0, zone)

        assertEquals(
            expected.toInstant().toEpochMilli() - now.toInstant().toEpochMilli(),
            WeeklyUpdateScheduler.delayUntilNextFridayMidnight(now),
        )
    }
}
