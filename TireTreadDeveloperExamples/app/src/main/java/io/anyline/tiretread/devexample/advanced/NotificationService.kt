package io.anyline.tiretread.devexample.advanced

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.anyline.tiretread.devexample.MainApplication
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.common.MeasurementResultData
import io.anyline.tiretread.devexample.common.MeasurementResultStatus
import io.anyline.tiretread.devexample.common.MeasurementResultUpdateInterface
import io.anyline.tiretread.devexample.common.TreadDepthResultStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : Service(), MeasurementResultUpdateInterface {

    companion object {
        const val NOTIF_ID = 1001
        const val NOTIF_DISMISS_ID = 1002
    }

    override fun onBind(p0: Intent?): IBinder? = null
    private val channelID = "notif_tiretread"
    private val label = "TireTread Measurements Notifications"

    private lateinit var notificationManager: NotificationManager

    private val measurementResultMap = mutableMapOf<String, MeasurementResultFile>()

    override fun onMeasurementResultDataStatusUpdate(
        measurementResultData: MeasurementResultData,
        measurementResultStatus: MeasurementResultStatus,
        customData: String?
    ) {
        when (measurementResultStatus) {
            is MeasurementResultStatus.ScanStarted -> {
                if (measurementResultMap[measurementResultData.measurementUUID] == null) {
                    val fileCustomData = customData?.let {
                        MeasurementResultCustomData.fromString(it)
                    } ?: run { MeasurementResultCustomData() }
                    measurementResultMap[measurementResultData.measurementUUID] =
                        MeasurementResultFile(
                            this@NotificationService,
                            MeasurementResultFile.FileData(measurementResultData, fileCustomData)
                        )
                }
                measurementResultMap[measurementResultData.measurementUUID]?.let {
                    it.fileData.measurementResultCustomData.notifyStatus(measurementResultStatus)
                    showOrUpdateNotification(NotificationEvent.MeasurementEvent(it.fileData))
                }
            }

            is MeasurementResultStatus.ScanAborted -> {
                removeNotification(measurementResultData.measurementUUID)
            }

            is MeasurementResultStatus.UploadCompleted -> {
                measurementResultMap[measurementResultData.measurementUUID]?.let {
                    it.fileData.measurementResultCustomData.notifyStatus(measurementResultStatus)
                    it.save()
                    showOrUpdateNotification(NotificationEvent.MeasurementEvent(it.fileData))
                    CoroutineScope(Dispatchers.IO).launch {
                        val treadDepthResultQueried =
                            MeasurementResultStatus.TreadDepthResultQueried(TreadDepthResultStatus.NotYetAvailable)
                        it.fileData.measurementResultData.measurementResultStatus =
                            treadDepthResultQueried
                        it.fileData.measurementResultCustomData.notifyStatus(treadDepthResultQueried)
                        showOrUpdateNotification(NotificationEvent.MeasurementEvent(it.fileData))

                        measurementResultData.getTreadDepthReportResult { treadDepthResultStatus ->
                            it.fileData.measurementResultCustomData.notifyStatus(
                                measurementResultStatus
                            )
                            it.save()
                            showOrUpdateNotification(NotificationEvent.MeasurementEvent(it.fileData))
                        }
                    }
                }
            }

            is MeasurementResultStatus.TreadDepthResultQueried -> {
                measurementResultMap[measurementResultData.measurementUUID]?.let {
                    it.fileData.measurementResultCustomData.notifyStatus(measurementResultStatus)
                    it.save()
                    showOrUpdateNotification(NotificationEvent.MeasurementEvent(it.fileData))
                }
            }

            else -> {
                measurementResultMap[measurementResultData.measurementUUID]?.let {
                    it.fileData.measurementResultCustomData.notifyStatus(measurementResultStatus)
                    showOrUpdateNotification(NotificationEvent.MeasurementEvent(it.fileData))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        (application as MainApplication).notificationService = this
    }

    override fun onDestroy() {
        (application as MainApplication).notificationService = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (intent != null) {
            if (intent.action.toString() == NOTIF_DISMISS_ID.toString()) {
                stopForeground(true)
                stopSelf()
            } else {
                val notification = showOrUpdateNotification(NotificationEvent.EmptyEvent)
                startForeground(NOTIF_ID, notification)
            }
        }
        return START_NOT_STICKY
    }

    sealed class NotificationEvent(
        val primaryText: String, val secondaryText: String
    ) {
        data object EmptyEvent : NotificationEvent("", "Watching Measurement Results")

        data class MeasurementEvent(val fileData: MeasurementResultFile.FileData) :
            NotificationEvent(
                fileData.getCaption(),
                fileData.measurementResultData.measurementResultStatus.statusDescription
            )

        fun silent(): Boolean {
            return when (this) {
                is EmptyEvent -> true
                is MeasurementEvent -> {
                    when (this.fileData.measurementResultData.measurementResultStatus) {
                        is MeasurementResultStatus.Error, is MeasurementResultStatus.TreadDepthResultQueried -> false
                        else -> true
                    }
                }
            }
        }

        fun priority(): Int {
            return when (silent()) {
                true -> Notification.PRIORITY_DEFAULT
                false -> Notification.PRIORITY_HIGH
            }
        }
    }

    private fun removeNotification(measurementUUID: String) {
        notificationManager.cancel(measurementUUID.hashCode())
    }

    private fun showOrUpdateNotification(event: NotificationEvent): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel(channelID, label, importance).apply {
                notificationManager.createNotificationChannel(this)
            }
        }

        val stopNotificationIntent = Intent(this, NotificationService::class.java)
        stopNotificationIntent.action = NOTIF_DISMISS_ID.toString()
        val dismissIntent = PendingIntent.getService(
            this,
            NOTIF_ID,
            stopNotificationIntent,
            PendingIntent.FLAG_IMMUTABLE // for android 12 support
        )

        val builder = NotificationCompat.Builder(this, channelID)
            .setSmallIcon(R.drawable.ic_notification_tire).setContentText(event.secondaryText)
            .setSubText(event.primaryText).setSilent(event.silent()).setPriority(event.priority())
            .setOngoing(event is NotificationEvent.EmptyEvent).setAutoCancel(
                when (event) {
                    is NotificationEvent.EmptyEvent -> false
                    is NotificationEvent.MeasurementEvent -> (event.fileData.measurementResultData.measurementResultStatus is MeasurementResultStatus.TreadDepthResultQueried)
                }
            ).setStyle(NotificationCompat.DecoratedCustomViewStyle())

        when (event) {
            is NotificationEvent.EmptyEvent -> {
                builder.addAction(
                    R.drawable.ic_notification_tire, "DISMISS", dismissIntent
                )
            }

            is NotificationEvent.MeasurementEvent -> {
                event.fileData.measurementResultData.measurementResultStatus.also { measurementResultStatus ->
                    when (measurementResultStatus) {
                        is MeasurementResultStatus.ImageUploaded -> {
                            builder.setProgress(
                                measurementResultStatus.total,
                                measurementResultStatus.uploaded,
                                false
                            )
                        }

                        is MeasurementResultStatus.UploadCompleted -> {
                            builder.setProgress(0, 0, true)
                        }

                        is MeasurementResultStatus.TreadDepthResultQueried -> {
                            when (measurementResultStatus.treadDepthResultStatus) {
                                TreadDepthResultStatus.NotYetAvailable -> {
                                    //waiting for tread depth result
                                }

                                is TreadDepthResultStatus.Succeed, is TreadDepthResultStatus.Failed -> {
                                    //tread depth result ready, add notification action
                                    val notifyIntent = MeasurementResultActivity.buildIntent(
                                        this, event.fileData.measurementResultData
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    val notifyPendingIntent = PendingIntent.getActivity(
                                        this,
                                        0,
                                        notifyIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    builder.addAction(
                                        R.drawable.ic_notification_tire,
                                        "View Measurement Results",
                                        notifyPendingIntent
                                    )
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        NotificationManagerCompat.from(this).apply {
            if (ActivityCompat.checkSelfPermission(
                    this@NotificationService, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@apply
            }

            notify(
                when (event) {
                    is NotificationEvent.EmptyEvent -> NOTIF_ID
                    is NotificationEvent.MeasurementEvent -> event.fileData.measurementResultData.measurementUUID.hashCode()
                }, builder.build()
            )
        }
        return builder.build()
    }

}

