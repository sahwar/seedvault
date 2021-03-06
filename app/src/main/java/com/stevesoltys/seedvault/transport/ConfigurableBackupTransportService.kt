package com.stevesoltys.seedvault.transport

import android.app.Service
import android.app.backup.BackupManager
import android.app.backup.BackupManager.FLAG_NON_INCREMENTAL_BACKUP
import android.app.backup.BackupTransport.FLAG_USER_INITIATED
import android.app.backup.IBackupManager
import android.content.Context
import android.content.Context.BACKUP_SERVICE
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.annotation.WorkerThread
import com.stevesoltys.seedvault.BackupMonitor
import com.stevesoltys.seedvault.BackupNotificationManager
import com.stevesoltys.seedvault.NotificationBackupObserver
import com.stevesoltys.seedvault.R
import org.koin.core.context.GlobalContext.get

private val TAG = ConfigurableBackupTransportService::class.java.simpleName

/**
 * @author Steve Soltys
 * @author Torsten Grote
 */
class ConfigurableBackupTransportService : Service() {

    private var transport: ConfigurableBackupTransport? = null

    override fun onCreate() {
        super.onCreate()
        transport = ConfigurableBackupTransport(applicationContext)
        Log.d(TAG, "Service created.")
    }

    override fun onBind(intent: Intent): IBinder {
        val transport = this.transport ?: throw IllegalStateException("no transport in onBind()")
        return transport.binder.apply {
            Log.d(TAG, "Transport bound.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        transport = null
        Log.d(TAG, "Service destroyed.")
    }

}

@WorkerThread
fun requestBackup(context: Context) {
    // show notification
    val nm: BackupNotificationManager = get().koin.get()
    nm.onBackupUpdate(context.getString(R.string.notification_backup_starting), 0, 1, true)

    val observer = NotificationBackupObserver(context, true)
    val flags = FLAG_NON_INCREMENTAL_BACKUP or FLAG_USER_INITIATED
    val packages = PackageService.eligiblePackages
    val result = try {
        val backupManager: IBackupManager = get().koin.get()
        backupManager.requestBackup(packages, observer, BackupMonitor(), flags)
    } catch (e: RemoteException) {
        Log.e(TAG, "Error during backup: ", e)
        nm.onBackupError()
    }
    if (result == BackupManager.SUCCESS) {
        Log.i(TAG, "Backup succeeded ")
    } else {
        Log.e(TAG, "Backup failed: $result")
    }
}
