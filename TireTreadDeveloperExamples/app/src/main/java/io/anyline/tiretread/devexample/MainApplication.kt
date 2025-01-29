package io.anyline.tiretread.devexample

import android.app.Application
import io.anyline.tiretread.devexample.advanced.NotificationService


class MainApplication : Application() {

    var notificationService: NotificationService? = null
}