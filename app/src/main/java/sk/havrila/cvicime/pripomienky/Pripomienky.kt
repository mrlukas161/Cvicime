package sk.havrila.cvicime.pripomienky

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import sk.havrila.cvicime.CvicimeApp
import sk.havrila.cvicime.MainActivity
import sk.havrila.cvicime.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Pripomienky sú viazané na KONTEXTOVÚ KOTVU ("keď uložím deti"), nie na tvrdý rozvrh.
 * Z techník zmeny správania vyšlo, že spúšťač naviazaný na existujúcu rutinu funguje
 * lepšie než pripomienka na hodinu - a hlavne, že najúčinnejšia zložka je riešenie
 * prekážky, nie samotné pripomenutie. Preto notifikácia vždy ponúka aj menšiu verziu.
 *
 * Plná logika (otázka na prekážku, skrátený tréning) patrí do etapy E2;
 * tu je zatiaľ len plánovanie a jednoduché zobrazenie.
 */
object Pripomienky {

    private const val KOD_DENNA = 2001

    fun naplanujDennu(context: Context, minutyOdPolnoci: Int) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(
            context,
            KOD_DENNA,
            Intent(context, PripomienkaReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (minutyOdPolnoci < 0) {
            am.cancel(intent)
            return
        }

        val teraz = LocalDateTime.now()
        var ciel = LocalDate.now().atStartOfDay().plusMinutes(minutyOdPolnoci.toLong())
        if (!ciel.isAfter(teraz)) ciel = ciel.plusDays(1)
        val kedy = ciel.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Presný alarm sa použije len ak ho systém povolí - inak radšej nepresný než pád.
        val mozePresny = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
        if (mozePresny) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, kedy, intent)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, kedy, intent)
        }
    }

    fun zobraz(context: Context, titulok: String, text: String) {
        val otvor = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val oznamenie = NotificationCompat.Builder(context, CvicimeApp.KANAL_PRIPOMIENKY)
            .setContentTitle(titulok)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_notifikacia)
            .setAutoCancel(true)
            .setContentIntent(otvor)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(KOD_DENNA, oznamenie)
        } catch (_: SecurityException) {
            // Používateľ nedal povolenie na notifikácie - to je jeho právo, nie chyba.
        }
    }
}

class PripomienkaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Pripomienky.zobraz(
            context,
            "Ideme?",
            "Ak nie je čas, dá sa aj dvanásťminútová verzia. Otvor a vyber si.",
        )
        // Preplánovanie na ďalší deň rieši obrazovka Nastavenia pri zmene kotvy;
        // tu ho zopakujeme, aby pripomienka nezanikla po prvom spustení.
        val prefs = context.getSharedPreferences("cvicime", Context.MODE_PRIVATE)
        val minuty = prefs.getInt("kotva_minuty", -1)
        if (minuty >= 0) Pripomienky.naplanujDennu(context, minuty)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("cvicime", Context.MODE_PRIVATE)
        val minuty = prefs.getInt("kotva_minuty", -1)
        if (minuty >= 0) Pripomienky.naplanujDennu(context, minuty)
    }
}
