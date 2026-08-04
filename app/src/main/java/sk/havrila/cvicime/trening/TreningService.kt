package sk.havrila.cvicime.trening

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import sk.havrila.cvicime.CvicimeApp
import sk.havrila.cvicime.MainActivity
import sk.havrila.cvicime.R

/**
 * Drží tréning nažive, kým je používateľ v prehrávači.
 *
 * Nepočíta odpočet - ten beží v prehrávači a je viazaný na [android.os.SystemClock.elapsedRealtime],
 * takže neuteká. Úlohou služby je len zabrániť tomu, aby systém appku uspal alebo zabil
 * uprostred série, keď si používateľ zhasne obrazovku alebo prepne na inú appku.
 *
 * Typ specialUse je zámerný - dataSync má na Androide 15 časový limit a health by
 * vyžadoval runtime oprávnenie na rozpoznávanie aktivity, ktoré nepotrebujeme.
 */
class TreningService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AKCIA_STOP -> {
                zastav()
                return START_NOT_STICKY
            }
        }

        val popis = intent?.getStringExtra(EXTRA_POPIS) ?: "Prebieha tréning"
        spustiVPopredi(popis)
        drzBdely()
        return START_STICKY
    }

    private fun spustiVPopredi(popis: String) {
        val oznamenie = postavOznamenie(popis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID_OZNAMENIA, oznamenie, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID_OZNAMENIA, oznamenie)
        }
    }

    private fun postavOznamenie(popis: String): Notification {
        val otvor = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, TreningService::class.java).setAction(AKCIA_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CvicimeApp.KANAL_TRENING)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(popis)
            .setSmallIcon(R.drawable.ic_notifikacia)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(otvor)
            .addAction(0, "Ukončiť", stop)
            .build()
    }

    @Suppress("WakelockTimeout")
    private fun drzBdely() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cvicime:trening").apply {
            setReferenceCounted(false)
            // Poistka: aj keby sa služba nezastavila korektne, wake lock po 3 h padne sám.
            acquire(3 * 60 * 60 * 1000L)
        }
    }

    private fun zastav() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val ID_OZNAMENIA = 1001
        private const val AKCIA_STOP = "sk.havrila.cvicime.STOP_TRENING"
        private const val EXTRA_POPIS = "popis"

        fun spusti(context: Context, popis: String) {
            val i = Intent(context, TreningService::class.java).putExtra(EXTRA_POPIS, popis)
            context.startForegroundService(i)
        }

        fun zastav(context: Context) {
            context.startService(Intent(context, TreningService::class.java).setAction(AKCIA_STOP))
        }
    }
}
