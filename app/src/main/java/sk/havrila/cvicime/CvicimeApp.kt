package sk.havrila.cvicime

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.katalog.KatalogLoader

class CvicimeApp : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        vytvorKanaly()
        scope.launch {
            // Celé zabalené: neošetrená výnimka v tejto korutine by zhodila appku pri štarte.
            runCatching {
                KatalogLoader.nacitajAkTreba(this@CvicimeApp)
                // Profil musí existovať od prvého spustenia, inak by sa UI muselo všade vetviť.
                Repozitar.ziskaj(this@CvicimeApp).profil()
            }.onFailure { android.util.Log.e("CvicimeApp", "Štart zlyhal: ${it.message}", it) }
        }
    }

    /**
     * Oba kanály sú zámerne tiché a s nízkou dôležitosťou.
     * Dôvod je praktický: appka sa používa doma, kde spia deti - pípajúca notifikácia
     * uprostred tréningu je najrýchlejšia cesta k tomu, aby ju používateľ odinštaloval.
     */
    private fun vytvorKanaly() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                KANAL_TRENING,
                getString(R.string.kanal_trening),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.kanal_trening_popis)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                KANAL_PRIPOMIENKY,
                getString(R.string.kanal_pripomienky),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.kanal_pripomienky_popis)
                enableVibration(true)
            }
        )
    }

    companion object {
        const val KANAL_TRENING = "trening"
        const val KANAL_PRIPOMIENKY = "pripomienky"
    }
}
