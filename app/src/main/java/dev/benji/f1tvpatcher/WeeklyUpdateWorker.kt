package dev.benji.f1tvpatcher

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class WeeklyUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        return try {
            val context = applicationContext
            val releaseSource = ReleaseSource(context)
            val release = releaseSource.fetchLatestRelease()
            UpdateRepository(context).recordRelease(release)

            val downloaded = ApkmInspector(context).inspect(release, releaseSource.download(release))
            val installed = InstalledAppInspector(context).inspect()
            val status = UpdateDecider.decide(installed, downloaded)
            if (status is UpdateStatus.UpdateAvailable ||
                status is UpdateStatus.OriginalOrUnknownInstalled
            ) {
                NotificationHelper(context).notifyUpdateAvailable(release)
            }
            Result.success()
        } catch (throwable: Throwable) {
            UpdateRepository(applicationContext).lastError = throwable.message
            Result.retry()
        }
    }
}
