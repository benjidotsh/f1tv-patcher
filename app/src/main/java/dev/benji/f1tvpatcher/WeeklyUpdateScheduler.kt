package dev.benji.f1tvpatcher

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object WeeklyUpdateScheduler {
    private const val WORK_NAME = "weekly-f1tv-patch-check"
    private val zone = ZoneId.of("Europe/Brussels")

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WeeklyUpdateWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNextFridayMidnight(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    internal fun delayUntilNextFridayMidnight(now: ZonedDateTime = ZonedDateTime.now(zone)): Long {
        var target = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            .with(LocalTime.MIDNIGHT)
        if (!target.isAfter(now)) {
            target = target.plusWeeks(1)
        }
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }
}
