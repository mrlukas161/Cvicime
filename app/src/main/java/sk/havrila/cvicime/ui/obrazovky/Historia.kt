package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.TreningEntity
import sk.havrila.cvicime.data.model.Blok
import sk.havrila.cvicime.ui.komponenty.Kapsula
import sk.havrila.cvicime.ui.komponenty.PrazdnyStav
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.formatujHodnotu
import sk.havrila.cvicime.ui.komponenty.formatujTrvanie
import sk.havrila.cvicime.ui.komponenty.sklonuj
import sk.havrila.cvicime.ui.theme.FarbyBlokov
import sk.havrila.cvicime.ui.theme.Grotesk
import java.time.LocalDate

/** Jedna séria v rozbalenom detaile tréningu. */
data class RiadokSerie(
    val blok: Blok,
    val cvikId: String,
    val cvikNazov: String,
    val meranie: String,
    val plan: Int,
    val skutocna: Int?,
    val rir: Int?,
    val preskocena: Boolean,
    /** séria je osobný rekord svojho cvičiaceho (najlepšia séria dňa nad doterajším maximom cviku) */
    val rekord: Boolean,
    /** kto sériu odcvičil: 0 = hlavný používateľ, inak id ďalšieho dospelého */
    val ucastnikId: Long = 0,
)

/** Cvičiaci, ktorý má v tréningu série - pre hlavičky v detaile. */
data class HistoriaCviciaci(val id: Long, val meno: String, val farba: Int?)

/** Tréning v Histórii so súhrnom a detailom sérií. */
data class HistoriaTrening(
    val trening: TreningEntity,
    /** odcvičené (nepreskočené) série všetkých cvičiacich */
    val pocetSerii: Int,
    /** odcvičené série podľa blokov v poradí blokov - farebný pásik pomeru */
    val serieBlokov: List<Pair<Blok, Int>>,
    val serie: List<RiadokSerie>,
    val pocetRekordov: Int,
    /** cvičiaci so sériami v tomto tréningu (hlavný používateľ prvý) */
    val cviciaci: List<HistoriaCviciaci> = emptyList(),
) {
    val trvanieMs: Long get() = trening.koniec?.let { (it - trening.zaciatok).coerceAtLeast(0L) } ?: 0L
}

/** Týždeň v Histórii; [pondelok] je epochDay. */
data class HistoriaTyzden(val pondelok: Long, val treningy: List<HistoriaTrening>) {
    val trvanieMs: Long get() = treningy.sumOf { it.trvanieMs }
}

class HistoriaModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repozitar.ziskaj(app)

    /** Názvy cvikov z katalógu - načítané raz, katalóg sa za behu nemení. */
    private val nazvyCvikov = mutableMapOf<String, String>()

    /** Tréningy zoskupené po týždňoch (najnovší prvý). null = ešte sa načítava. */
    val tyzdne: StateFlow<List<HistoriaTyzden>?> = repo.denik.posledneTreningyTok(90)
        .map { zostavTyzdne(it) }
        // Zoskupovanie a rekordy z celej histórie nech nebrzdia hlavné vlákno.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private suspend fun zostavTyzdne(treningy: List<TreningEntity>): List<HistoriaTyzden> {
        if (treningy.isEmpty()) return emptyList()
        val serieTreningov = treningy.associate { it.id to repo.denik.serieTreningu(it.id) }
        // Ďalší dospelí (0.7): mená a farby pre hlavičky v detaile; neaktívny ostane bez mena.
        val ucastnici = repo.ucastnici().associateBy { it.id }
        val ktoCvicil = serieTreningov.values.flatten().map { it.ucastnikId }.toSet() + 0L
        // Rekordy rovnakým výpočtom ako na Pokroku (najlepšia séria dňa nad doterajším maximom cviku),
        // každý cvičiaci voči vlastnej histórii.
        val rekordyPodlaDna = ktoCvicil.associateWith { id ->
            repo.posledneRekordy(Int.MAX_VALUE, id).groupBy { it.epochDen }
        }
        val karty = treningy.map { t ->
            val serie = serieTreningov[t.id].orEmpty()
            val rekordneSerie = mutableSetOf<Long>()
            for ((ucastnikId, podlaDna) in rekordyPodlaDna) {
                podlaDna[t.epochDen].orEmpty().forEach { r ->
                    serie.firstOrNull {
                        !it.preskocena && it.ucastnikId == ucastnikId && it.cvikId == r.cvikId &&
                            it.skutocnaHodnota == r.hodnota
                    }?.let { rekordneSerie += it.id }
                }
            }
            val riadky = serie.map { s ->
                RiadokSerie(
                    blok = Blok.z(s.blok),
                    cvikId = s.cvikId,
                    cvikNazov = nazvyCvikov.getOrPut(s.cvikId) { repo.katalog.cvik(s.cvikId)?.nazov ?: s.cvikId },
                    meranie = s.meranie,
                    plan = s.planHodnota,
                    skutocna = s.skutocnaHodnota,
                    rir = s.rir,
                    preskocena = s.preskocena,
                    rekord = s.id in rekordneSerie,
                    ucastnikId = s.ucastnikId,
                )
            }
            val odcvicene = riadky.filter { !it.preskocena && it.skutocna != null }
            HistoriaTrening(
                trening = t,
                pocetSerii = odcvicene.size,
                serieBlokov = odcvicene.groupingBy { it.blok }.eachCount().toList().sortedBy { it.first.poradie },
                serie = riadky,
                pocetRekordov = rekordneSerie.size,
                cviciaci = riadky.map { it.ucastnikId }.distinct().sorted().map { id ->
                    val u = ucastnici[id]
                    HistoriaCviciaci(
                        id = id,
                        meno = when {
                            id == 0L -> "Ty"
                            u != null -> u.meno
                            else -> "Ďalší cvičiaci"
                        },
                        farba = u?.farba,
                    )
                },
            )
        }
        return karty
            .groupBy { repo.zaciatokTyzdna(it.trening.epochDen) }
            .map { (pondelok, zoznam) -> HistoriaTyzden(pondelok, zoznam) }
    }
}

@Composable
fun HistoriaObrazovka(model: HistoriaModel = viewModel()) {
    val tyzdne by model.tyzdne.collectAsStateWithLifecycle()
    var rozbalene by remember { mutableStateOf(setOf<Long>()) }
    val dnes = remember { LocalDate.now() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "titulok") {
            Text("História", style = MaterialTheme.typography.headlineLarge)
        }

        val zoznam = tyzdne
        if (zoznam != null && zoznam.isEmpty()) {
            item(key = "prazdne") {
                PrazdnyStav(
                    titulok = "Zatiaľ nič",
                    text = "Prvý tréning sa tu objaví hneď po dokončení.",
                    rebrikPostavicky = "drep",
                )
            }
        }

        zoznam?.forEach { tyzden ->
            item(key = "tyzden-${tyzden.pondelok}") {
                HlavickaTyzdna(tyzden, dnes, Modifier.animateItem())
            }
            items(tyzden.treningy, key = { it.trening.id }) { k ->
                val id = k.trening.id
                KartaTreningu(
                    k = k,
                    rozbaleny = id in rozbalene,
                    naKlik = { rozbalene = if (id in rozbalene) rozbalene - id else rozbalene + id },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ týždeň

private val NAZVY_DNI_HISTORIA = listOf("Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok", "Sobota", "Nedeľa")

/** "6.–12. 10.", "29. 9.–5. 10.", v inom roku s rokom. */
private fun rozsahTyzdna(pondelok: Long, dnes: LocalDate): String {
    val od = LocalDate.ofEpochDay(pondelok)
    val po = od.plusDays(6)
    val rok = if (od.year != dnes.year || po.year != dnes.year) " ${po.year}" else ""
    return when {
        od.year != po.year ->
            "${od.dayOfMonth}. ${od.monthValue}. ${od.year}–${po.dayOfMonth}. ${po.monthValue}. ${po.year}"
        od.monthValue == po.monthValue ->
            "${od.dayOfMonth}.–${po.dayOfMonth}. ${po.monthValue}.$rok"
        else ->
            "${od.dayOfMonth}. ${od.monthValue}.–${po.dayOfMonth}. ${po.monthValue}.$rok"
    }
}

@Composable
private fun HlavickaTyzdna(t: HistoriaTyzden, dnes: LocalDate, modifier: Modifier = Modifier) {
    val n = t.treningy.size
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)) {
            append(rozsahTyzdna(t.pondelok, dnes))
        }
        append(" · $n ${sklonuj(n, "tréning", "tréningy", "tréningov")}")
        if (t.trvanieMs >= 60_000L) append(" · ${formatujTrvanie(t.trvanieMs)}")
    }
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

// ------------------------------------------------------------------ karta tréningu

@Composable
private fun KartaTreningu(
    k: HistoriaTrening,
    rozbaleny: Boolean,
    naKlik: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = k.trening
    val datum = LocalDate.ofEpochDay(t.epochDen)
    val sipka by animateFloatAsState(if (rozbaleny) 180f else 0f, label = "sipka")
    val farbaIkony = if (t.rodinny) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    TonovanaKarta(modifier = modifier, onClick = naKlik, vnutorneOdsadenie = 16.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(farbaIkony.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (t.rodinny) Icons.Filled.FamilyRestroom else Icons.Filled.FitnessCenter,
                        contentDescription = if (t.rodinny) "Rodinný tréning" else null,
                        tint = farbaIkony,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${NAZVY_DNI_HISTORIA[datum.dayOfWeek.value - 1]}, ${datum.dayOfMonth}. ${datum.monthValue}." +
                            (if (datum.year != LocalDate.now().year) " ${datum.year}" else ""),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            append("%02d:%02d".format(t.minutyDna / 60, t.minutyDna % 60))
                            if (k.trvanieMs >= 60_000L) append(" · ${formatujTrvanie(k.trvanieMs)}")
                            append(" · ${k.pocetSerii} ${sklonuj(k.pocetSerii, "séria", "série", "sérií")}")
                            if (t.rodinny) append(" · s deťmi")
                            val dalsi = k.cviciaci.filter { it.id != 0L }
                            if (dalsi.isNotEmpty()) append(" · aj ${dalsi.joinToString(", ") { it.meno }}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (k.pocetRekordov > 0) {
                    Spacer(Modifier.width(8.dp))
                    Kapsula("${k.pocetRekordov}", MaterialTheme.colorScheme.primary, ikona = Icons.Filled.EmojiEvents)
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (rozbaleny) "Zbaliť" else "Rozbaliť",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(sipka),
                )
            }
            if (k.serieBlokov.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                PasBlokov(k.serieBlokov)
            }
            if (rozbaleny) {
                Spacer(Modifier.height(16.dp))
                if (k.cviciaci.size > 1) {
                    // Viac dospelých na jednom tréningu: každý má vlastný blok sérií.
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        k.cviciaci.forEach { c ->
                            Column {
                                HlavickaCviciaceho(c)
                                Spacer(Modifier.height(10.dp))
                                DetailTreningu(k.serie.filter { it.ucastnikId == c.id })
                            }
                        }
                    }
                } else {
                    DetailTreningu(k.serie)
                }
            }
        }
    }
}

/** Pomer odcvičených sérií podľa blokov - farebný jazyk tréningu na jeden pohľad. */
@Composable
private fun PasBlokov(podiely: List<Pair<Blok, Int>>) {
    Row(
        Modifier.fillMaxWidth().height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        podiely.forEach { (blok, pocet) ->
            Box(
                Modifier
                    .weight(pocet.toFloat())
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(FarbyBlokov.pre(blok))
            )
        }
    }
}

// ------------------------------------------------------------------ detail

@Composable
private fun HlavickaCviciaceho(c: HistoriaCviciaci) {
    val farba = c.farba?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(farba.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                c.meno.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(c.meno, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk))
    }
}

@Composable
private fun DetailTreningu(serie: List<RiadokSerie>) {
    if (serie.isEmpty()) {
        Text(
            "Žiadne zapísané série.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Bloky v poradí, v akom sa cvičili; v bloku cviky v poradí prvého výskytu (aj pri kolotoči).
        serie.groupBy { it.blok }.forEach { (blok, serieBloku) ->
            val farba = FarbyBlokov.pre(blok)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(farba))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        blok.nazov.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = Grotesk),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                serieBloku.groupBy { it.cvikId }.forEach { (_, serieCviku) ->
                    Text(
                        serieCviku.first().cvikNazov,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp),
                    )
                    serieCviku.forEachIndexed { i, s -> RiadokSerieDetail(i + 1, s, farba) }
                }
            }
        }
    }
}

@Composable
private fun RiadokSerieDetail(poradie: Int, s: RiadokSerie, farba: Color) {
    val skutocna = s.skutocna
    val hodnota = skutocna ?: s.plan
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(farba.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$poradie",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (s.preskocena) {
            Text(
                "preskočená",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                formatujHodnotu(hodnota, s.meranie),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Grotesk),
                color = if (skutocna != null && skutocna > s.plan) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(min = 56.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                popisSerie(s),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (s.rekord) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = "Osobný rekord",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** "plán 12× · na doraz", "podľa plánu · v zálohe 1–2". */
private fun popisSerie(s: RiadokSerie): String {
    val casti = mutableListOf<String>()
    val skutocna = s.skutocna
    casti += if (skutocna == null || skutocna == s.plan) "podľa plánu" else "plán ${formatujHodnotu(s.plan, s.meranie)}"
    when (s.rir) {
        0 -> casti += "na doraz"
        1 -> casti += "v zálohe 1–2"
        3 -> casti += "v zálohe 3+"
    }
    return casti.joinToString(" · ")
}
