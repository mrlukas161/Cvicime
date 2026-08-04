package sk.havrila.cvicime.data

import android.content.Context
import sk.havrila.cvicime.data.denik.CielEntity
import sk.havrila.cvicime.data.denik.DenikDatabase
import sk.havrila.cvicime.data.denik.DietaEntity
import sk.havrila.cvicime.data.denik.ProfilEntity
import sk.havrila.cvicime.data.denik.SeriaEntity
import sk.havrila.cvicime.data.denik.SeriaPartiaEntity
import sk.havrila.cvicime.data.denik.TestEntity
import sk.havrila.cvicime.data.denik.TreningEntity
import sk.havrila.cvicime.data.denik.UrovenRebrikaEntity
import sk.havrila.cvicime.data.denik.ZvieraEntity
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.KatalogDatabase
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.katalog.rozparsujPartie
import sk.havrila.cvicime.data.model.DruhCiela
import sk.havrila.cvicime.data.model.Naradie
import sk.havrila.cvicime.data.model.Partia
import sk.havrila.cvicime.domain.Generator
import sk.havrila.cvicime.domain.PlanTreningu
import sk.havrila.cvicime.domain.Pravidla
import sk.havrila.cvicime.domain.VstupGeneratora
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Jediné miesto, cez ktoré appka pracuje s dátami.
 * Drží dohromady katalóg (knižnica cvikov) a denník (história používateľa).
 */
class Repozitar(context: Context) {

    private val app = context.applicationContext
    val katalog = KatalogDatabase.ziskaj(app).dao()
    val denik = DenikDatabase.ziskaj(app).dao()
    val prefs = Prefs(app)

    // ---------------------------------------------------------------- profil a ciele

    suspend fun profil(): ProfilEntity = denik.profil() ?: ProfilEntity().also { denik.ulozProfil(it) }

    fun profilTok() = denik.profilTok()

    suspend fun ulozProfil(profil: ProfilEntity) = denik.ulozProfil(profil)

    suspend fun naradie(): Set<Naradie> =
        profil().naradie.split(",").mapNotNull { Naradie.z(it.trim()) }.toSet() + Naradie.ZIADNE

    fun cieleTok() = denik.aktivneCieleTok()

    suspend fun ulozCiel(ciel: CielEntity) = denik.ulozCiel(ciel)

    suspend fun zmazCiel(kluc: String) = denik.zmazCiel(kluc)

    /** Odporúčaný denný príjem bielkovín. 1,8 g/kg je stred pásma 1,6–2,2 z meta-analýzy. */
    fun odporucaneBielkoviny(vahaKg: Double): Int = Math.round(vahaKg * 1.8).toInt()

    // ---------------------------------------------------------------- katalóg

    fun rebrikyTok() = katalog.rebrikyTok()

    suspend fun rebriky() = katalog.rebriky()

    suspend fun cvikyRebrika(rebrikId: String) = katalog.cvikyRebrika(rebrikId)

    suspend fun cvik(id: String) = katalog.cvik(id)

    suspend fun urovenVRebriku(rebrikId: String): Int = denik.uroven(rebrikId)?.uroven ?: 1

    suspend fun nastavUroven(rebrikId: String, uroven: Int) {
        denik.ulozUroven(
            UrovenRebrikaEntity(
                rebrikId = rebrikId,
                uroven = uroven,
                zmeneneEpochDen = dnes(),
            )
        )
    }

    // ---------------------------------------------------------------- generovanie tréningu

    suspend fun zostavPlan(dostupneMinuty: Int): PlanTreningu {
        val profil = profil()
        val rebriky = katalog.rebriky()
        val cvikyPodlaRebrika = rebriky.associate { it.id to katalog.cvikyRebrika(it.id) }
        val urovne = denik.urovne().associate { it.rebrikId to it.uroven }
        val ciele = denik.aktivneCiele()

        val cieleZrucnosti = ciele.filter { DruhCiela.z(it.druh) == DruhCiela.ZRUCNOST }
            .sortedBy { it.priorita }
            .map { it.kluc }
        val prioritnePartie = ciele.filter { DruhCiela.z(it.druh) == DruhCiela.PARTIA }
            .mapNotNull { Partia.z(it.kluc) }
            .toSet()

        val dnesDen = dnes()
        val zaciatokTyzdna = zaciatokTyzdna(dnesDen)
        val objem = denik.objemPodlaPartii(zaciatokTyzdna, dnesDen)
            .mapNotNull { o -> Partia.z(o.partia)?.let { it to o.serie } }
            .toMap()
        val poslednaZataz = denik.poslednaZatazPartii()
            .mapNotNull { z -> Partia.z(z.partia)?.let { it to z.den } }
            .toMap()

        val tyzdnov = if (profil.zaciatokEpochDen > 0) {
            ((dnesDen - profil.zaciatokEpochDen) / 7).toInt().coerceAtLeast(0)
        } else {
            0
        }

        return Generator.zostav(
            VstupGeneratora(
                dostupneMinuty = dostupneMinuty,
                naradie = naradie(),
                urovne = urovne,
                cieleZrucnosti = cieleZrucnosti,
                prioritnePartie = prioritnePartie,
                objemTyzdna = objem,
                poslednaZataz = poslednaZataz,
                dnesEpochDen = dnesDen,
                tyzdnovOdZaciatku = tyzdnov,
                deload = potrebujeDeload(),
                rebriky = rebriky,
                cvikyPodlaRebrika = cvikyPodlaRebrika,
            )
        )
    }

    /**
     * Deload je heuristika, nie pravidlo - publikované číselné kritériá neexistujú.
     * Preto je zámerne konzervatívny: musia byť DVA dokončené tréningy po sebe,
     * v ktorých väčšina silových sérií skončila na doraz.
     */
    private suspend fun potrebujeDeload(): Boolean {
        val posledne = denik.posledneTreningy(2)
        if (posledne.size < 2) return false
        val serie = posledne.map { denik.serieTreningu(it.id) }
        return Pravidla.potrebujeDeload(serie)
    }

    // ---------------------------------------------------------------- priebeh tréningu

    suspend fun zacniTrening(planovaneMinuty: Int, rodinny: Boolean): Long {
        val teraz = LocalDateTime.now()
        val id = denik.vlozTrening(
            TreningEntity(
                zaciatok = System.currentTimeMillis(),
                epochDen = teraz.toLocalDate().toEpochDay(),
                minutyDna = teraz.toLocalTime().toSecondOfDay() / 60,
                planovaneMinuty = planovaneMinuty,
                rodinny = rodinny,
            )
        )
        prefs.beziaciTreningId = id
        return id
    }

    suspend fun zapisSeriu(
        treningId: Long,
        blok: String,
        poradie: Int,
        cvik: CvikEntity,
        planHodnota: Int,
        skutocnaHodnota: Int?,
        rir: Int?,
        preskocena: Boolean = false,
    ) {
        val den = dnes()
        val seriaId = denik.vlozSeriu(
            SeriaEntity(
                treningId = treningId,
                epochDen = den,
                blok = blok,
                poradie = poradie,
                cvikId = cvik.id,
                rebrikId = cvik.rebrikId,
                uroven = cvik.uroven,
                meranie = cvik.meranie,
                partie = cvik.partie,
                planHodnota = planHodnota,
                skutocnaHodnota = skutocnaHodnota,
                rir = rir,
                preskocena = preskocena,
            )
        )
        val rozpad = rozparsujPartie(cvik.partie).map { (kluc, vaha) ->
            SeriaPartiaEntity(seriaId = seriaId, partia = kluc, vaha = vaha)
        }
        if (rozpad.isNotEmpty()) denik.vlozSeriaPartie(rozpad)
    }

    suspend fun ukonciTrening(treningId: Long, pocitPo: Int?) {
        val t = denik.trening(treningId) ?: return
        denik.aktualizujTrening(
            t.copy(koniec = System.currentTimeMillis(), pocitPo = pocitPo, dokonceny = true)
        )
        prefs.beziaciTreningId = -1
        vyhodnotPostupy(treningId)
    }

    /**
     * Po tréningu prehodnotí, či sa v niektorom rebríku posunúť o stupeň.
     * Progresia pri vlastnej váhe je ťažšia variácia, nie viac opakovaní donekonečna.
     */
    suspend fun vyhodnotPostupy(treningId: Long): List<Pair<String, Int>> {
        val zmeny = mutableListOf<Pair<String, Int>>()
        val serie = denik.serieTreningu(treningId).filter { it.blok == "sila" || it.blok == "skill" }
        for ((rebrikId, serieRebrika) in serie.groupBy { it.rebrikId }) {
            val cvik = katalog.cvik(serieRebrika.first().cvikId) ?: continue
            val max = katalog.maxUroven(rebrikId) ?: continue
            val aktualna = urovenVRebriku(rebrikId)
            when {
                Pravidla.maPostupitVyssie(serieRebrika, cvik) && aktualna < max -> {
                    nastavUroven(rebrikId, aktualna + 1)
                    zmeny += rebrikId to (aktualna + 1)
                }
                Pravidla.maKlesnutNizsie(serieRebrika, cvik) && aktualna > 1 -> {
                    nastavUroven(rebrikId, aktualna - 1)
                    zmeny += rebrikId to (aktualna - 1)
                }
            }
        }
        return zmeny
    }

    // ---------------------------------------------------------------- testy

    suspend fun zapisTest(cvik: CvikEntity, hodnota: Int, jeVstupny: Boolean) {
        val teraz = LocalDateTime.now()
        denik.vlozTest(
            TestEntity(
                epochDen = teraz.toLocalDate().toEpochDay(),
                minutyDna = teraz.toLocalTime().toSecondOfDay() / 60,
                rebrikId = cvik.rebrikId,
                cvikId = cvik.id,
                uroven = cvik.uroven,
                meranie = cvik.meranie,
                hodnota = hodnota,
                jeVstupny = jeVstupny,
            )
        )
    }

    /**
     * Sila kolíše počas dňa. Ak sa retest robí v inú dennú dobu ako pôvodný test,
     * čísla sa nedajú poctivo porovnať - appka to musí povedať, nie to zamlčať.
     */
    suspend fun upozornenieNaDennuDobu(): String? {
        val prvy = denik.prvyVstupnyTest() ?: return null
        val teraz = LocalTime.now().toSecondOfDay() / 60
        val rozdiel = kotlin.math.abs(teraz - prvy.minutyDna)
        if (rozdiel <= Pravidla.TOLERANCIA_DENNEJ_DOBY_MINUT) return null
        val h = prvy.minutyDna / 60
        val m = prvy.minutyDna % 60
        return "Pôvodný test si robil o %02d:%02d. Zmeraj sa v podobný čas — sila cez deň kolíše " .format(h, m) +
            "a inak by graf ukázal progres, ktorý žiadny nie je."
    }

    suspend fun jeCasNaRetest(): Boolean {
        val posledny = denik.poslednyTestDen() ?: return false
        return dnes() - posledny >= Pravidla.RETEST_PO_DNOCH
    }

    // ---------------------------------------------------------------- deti

    fun detiTok() = denik.detiTok()

    suspend fun ulozDieta(dieta: DietaEntity) = denik.ulozDieta(dieta)

    suspend fun odomkniZviera(dietaId: Long, zvieraKluc: String) {
        denik.odomkniZviera(
            ZvieraEntity(dietaId = dietaId, zvieraKluc = zvieraKluc, odomknuteEpochDen = dnes())
        )
    }

    fun zvierataTok() = denik.vsetkyZvierataTok()

    // ---------------------------------------------------------------- pomocné

    fun dnes(): Long = LocalDate.now().toEpochDay()

    fun zaciatokTyzdna(den: Long): Long {
        val d = LocalDate.ofEpochDay(den)
        return d.minusDays((d.dayOfWeek.value - 1).toLong()).toEpochDay()
    }

    companion object {
        @Volatile
        private var instancia: Repozitar? = null

        fun ziskaj(context: Context): Repozitar =
            instancia ?: synchronized(this) {
                instancia ?: Repozitar(context).also { instancia = it }
            }
    }
}
