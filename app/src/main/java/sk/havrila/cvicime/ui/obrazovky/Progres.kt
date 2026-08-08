package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.BodGrafu
import sk.havrila.cvicime.data.denik.MieraEntity
import sk.havrila.cvicime.data.model.DruhCiela
import sk.havrila.cvicime.data.model.Partia
import sk.havrila.cvicime.domain.Objem
import sk.havrila.cvicime.domain.Pokrok
import sk.havrila.cvicime.domain.StavCiela

data class ObjemRiadok(val partia: Partia, val odcvicene: Double, val ciel: Double, val prioritna: Boolean)

data class GrafRebrika(val rebrikId: String, val nazov: String, val body: List<BodGrafu>, val jednotka: String)

class ProgresModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _ciele = MutableStateFlow<List<StavCiela>>(emptyList())
    val ciele = _ciele.asStateFlow()

    private val _objem = MutableStateFlow<List<ObjemRiadok>>(emptyList())
    val objem = _objem.asStateFlow()

    private val _grafy = MutableStateFlow<List<GrafRebrika>>(emptyList())
    val grafy = _grafy.asStateFlow()

    private val _miery = MutableStateFlow<Map<String, List<MieraEntity>>>(emptyMap())
    val miery = _miery.asStateFlow()

    private val _retest = MutableStateFlow<String?>(null)
    val retest = _retest.asStateFlow()

    init {
        obnov()
    }

    fun obnov() {
        viewModelScope.launch {
            val urovne = repo.denik.urovne().associate { it.rebrikId to it.uroven }
            val maxy = repo.katalog.maxUrovne().associate { it.rebrikId to it.maxUroven }
            val aktivne = repo.denik.aktivneCiele()

            _ciele.value = aktivne
                .filter { DruhCiela.z(it.druh) == DruhCiela.ZRUCNOST }
                .map { Pokrok.vyhodnotZrucnost(it.kluc, it.nazov, urovne, maxy) }

            val prioritne = aktivne.filter { DruhCiela.z(it.druh) == DruhCiela.PARTIA }
                .mapNotNull { Partia.z(it.kluc) }.toSet()
            val dnes = repo.dnes()
            val tyzdenObjem = repo.denik.objemPodlaPartii(repo.zaciatokTyzdna(dnes), dnes)
                .mapNotNull { o -> Partia.z(o.partia)?.let { it to o.serie } }.toMap()
            val profil = repo.profil()
            val tyzdnov = if (profil.zaciatokEpochDen > 0) ((dnes - profil.zaciatokEpochDen) / 7).toInt() else 0
            _objem.value = Partia.entries
                .map { p ->
                    ObjemRiadok(
                        partia = p,
                        odcvicene = tyzdenObjem[p] ?: 0.0,
                        ciel = Objem.cielSerii(p, p in prioritne, tyzdnov),
                        prioritna = p in prioritne,
                    )
                }
                .filter { it.prioritna || it.odcvicene > 0 }
                .sortedByDescending { it.prioritna }

            // Grafy: rebríky cieľov + tie, kde je aspoň 2× zapísaná séria.
            val rebriky = repo.rebriky().associateBy { it.id }
            val kandidati = (
                aktivne.filter { DruhCiela.z(it.druh) == DruhCiela.ZRUCNOST }.map { it.kluc } +
                    listOf("kliky", "pritahy", "drep")
                ).distinct()
            _grafy.value = kandidati.mapNotNull { id ->
                val body = repo.denik.grafRebrika(id)
                if (body.size < 2) return@mapNotNull null
                val r = rebriky[id] ?: return@mapNotNull null
                val jednotka = if (repo.cvikyRebrika(id).firstOrNull()?.meranie == "vydrz") "s" else "×"
                GrafRebrika(id, r.nazov, body, jednotka)
            }

            _miery.value = repo.denik.vsetkyMiery().groupBy { it.typ }
            _retest.value = if (repo.jeCasNaRetest()) repo.upozornenieNaDennuDobu()
                ?: "Od posledného testu prešli 4 týždne — oplatí sa premerať."
            else null
        }
    }

    fun pridajMieru(typ: String, hodnota: Double) {
        viewModelScope.launch {
            repo.denik.vlozMieru(MieraEntity(epochDen = repo.dnes(), typ = typ, hodnota = hodnota))
            obnov()
        }
    }
}

@Composable
fun ProgresObrazovka(naTest: () -> Unit, model: ProgresModel = viewModel()) {
    val ciele by model.ciele.collectAsStateWithLifecycle()
    val objem by model.objem.collectAsStateWithLifecycle()
    val grafy by model.grafy.collectAsStateWithLifecycle()
    val miery by model.miery.collectAsStateWithLifecycle()
    val retest by model.retest.collectAsStateWithLifecycle()

    var dialogMiera by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) { model.obnov() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Pokrok", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        retest?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Čas na retest", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = naTest) { Text("Premerať sa") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (ciele.isEmpty() && objem.isEmpty()) {
            Text(
                "Zatiaľ tu nie je čo ukázať — pokrok sa objaví po prvých tréningoch. " +
                    "Ciele si nastavíš v Nastaveniach.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---------------- ciele so zručnosťami ----------------
        ciele.forEach { c ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            c.nazov.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${(c.pokrok * 100).toInt()} %",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    c.brany.forEach { b ->
                        val brzda = b == c.brzda
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                b.brana.nazov,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                color = if (brzda) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${b.aktualnaUroven}/${b.potrebnaUroven}" + if (brzda) "  ← brzda" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (brzda) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { b.pokrok.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        )
                    }
                    Pokrok.vetaBrzdy(c)?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---------------- týždenný objem ----------------
        if (objem.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Tento týždeň (série na partiu)", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            objem.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        r.partia.nazov + if (r.prioritna) " ★" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${"%.1f".format(r.odcvicene).removeSuffix(".0").removeSuffix(",0")} / ${r.ciel.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { (r.odcvicene / r.ciel).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
        }

        // ---------------- grafy sily ----------------
        if (grafy.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Sila v čase", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Najlepšia séria dňa. Zmena o 1 opakovanie je bežné kolísanie, nie pokrok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            grafy.forEach { g ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row {
                            Text(g.nazov, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Text(
                                "${g.body.last().hodnota}${g.jednotka}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        CiaraGrafu(g.body)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        // ---------------- miery ----------------
        Spacer(Modifier.height(16.dp))
        Text("Miery", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pri lýtkach a predlaktiach je obvod jediné poctivé meradlo. Raz mesačne stačí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (miery.isEmpty()) {
            Text("Zatiaľ žiadne.", style = MaterialTheme.typography.bodyMedium)
        } else {
            miery.forEach { (typ, zoznam) ->
                val posledna = zoznam.last()
                val prva = zoznam.first()
                val delta = posledna.hodnota - prva.hodnota
                Row {
                    Text(nazovMiery(typ), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "%.1f cm".format(posledna.hodnota).replace(".", ",") +
                            if (zoznam.size > 1) "  (%+.1f)".format(delta).replace(".", ",") else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { dialogMiera = true }) { Text("Zapísať mieru") }
        Spacer(Modifier.height(24.dp))
    }

    if (dialogMiera) {
        DialogMiery(
            zavri = { dialogMiera = false },
            uloz = { typ, hodnota -> model.pridajMieru(typ, hodnota); dialogMiera = false },
        )
    }
}

private fun nazovMiery(typ: String) = when (typ) {
    "lytko" -> "Lýtko"
    "predlaktie" -> "Predlaktie"
    "hrudnik" -> "Hrudník"
    "pas" -> "Pás"
    "rameno" -> "Rameno (biceps)"
    else -> typ
}

@Composable
private fun DialogMiery(zavri: () -> Unit, uloz: (String, Double) -> Unit) {
    var typ by remember { mutableStateOf("lytko") }
    var hodnota by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = zavri,
        title = { Text("Nová miera") },
        text = {
            Column {
                listOf("lytko", "predlaktie", "rameno", "hrudnik", "pas").forEach { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                    ) {
                        androidx.compose.material3.RadioButton(selected = typ == t, onClick = { typ = t })
                        Text(nazovMiery(t))
                    }
                }
                OutlinedTextField(
                    value = hodnota,
                    onValueChange = { hodnota = it.filter { z -> z.isDigit() || z == '.' || z == ',' } },
                    label = { Text("Obvod (cm)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                hodnota.replace(",", ".").toDoubleOrNull()?.let { uloz(typ, it) }
            }) { Text("Uložiť") }
        },
        dismissButton = { TextButton(onClick = zavri) { Text("Zrušiť") } },
    )
}

/** Jednoduchý čiarový graf - úmyselne bez knižnice, netreba nič viac. */
@Composable
private fun CiaraGrafu(body: List<BodGrafu>) {
    val farba = MaterialTheme.colorScheme.primary
    val mriezka = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        if (body.size < 2) return@Canvas
        val minD = body.first().den.toFloat()
        val maxD = body.last().den.toFloat().coerceAtLeast(minD + 1)
        val minH = (body.minOf { it.hodnota } - 1).toFloat()
        val maxH = (body.maxOf { it.hodnota } + 1).toFloat()
        fun bod(b: BodGrafu) = Offset(
            x = (b.den - minD) / (maxD - minD) * size.width,
            y = size.height - (b.hodnota - minH) / (maxH - minH) * size.height,
        )
        drawLine(mriezka, Offset(0f, size.height), Offset(size.width, size.height), 2f)
        body.zipWithNext().forEach { (a, b) ->
            drawLine(farba, bod(a), bod(b), 6f, StrokeCap.Round)
        }
        body.forEach { drawCircle(farba, radius = 8f, center = bod(it)) }
    }
}
