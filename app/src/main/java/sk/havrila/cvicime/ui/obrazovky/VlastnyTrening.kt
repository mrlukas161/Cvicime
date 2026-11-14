package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.PREFIX_VLASTNY
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.VlastnyCvikEntity
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.domain.Pravidla
import sk.havrila.cvicime.domain.StylTreningu
import sk.havrila.cvicime.domain.VlastnaPolozka
import sk.havrila.cvicime.ui.komponenty.NadpisSekcie
import sk.havrila.cvicime.ui.komponenty.Postavicka
import sk.havrila.cvicime.ui.komponenty.TonovanaKarta
import sk.havrila.cvicime.ui.komponenty.VelkeTlacidlo
import sk.havrila.cvicime.ui.komponenty.rememberHaptika

/** Riadok editora: položka + cvik (kvôli názvu a meraniu). */
data class RiadokVlastneho(val polozka: VlastnaPolozka, val cvik: CvikEntity)

class VlastnyTreningModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _nazov = MutableStateFlow("")
    val nazov = _nazov.asStateFlow()
    private val _styl = MutableStateFlow(StylTreningu.KLASIKA)
    val styl = _styl.asStateFlow()
    private val _riadky = MutableStateFlow<List<RiadokVlastneho>>(emptyList())
    val riadky = _riadky.asStateFlow()

    /** Všetky cviky na výber: knižnica (bez detských) + vlastné. */
    private val _ponuka = MutableStateFlow<List<CvikEntity>>(emptyList())
    val ponuka = _ponuka.asStateFlow()

    private var id = 0L
    private var nacitane = false

    fun nacitaj(id: Long) {
        if (nacitane) return
        nacitane = true
        this.id = id
        viewModelScope.launch {
            obnovPonuku()
            val t = if (id > 0) repo.vlastnyTrening(id) else null
            if (t != null) {
                _nazov.value = t.nazov
                _styl.value = StylTreningu.z(t.styl)
                _riadky.value = repo.polozkyVlastneho(t).mapNotNull { p -> repo.cvikPreVlastny(p.cvikId)?.let { RiadokVlastneho(p, it) } }
            }
        }
    }

    private suspend fun obnovPonuku() {
        val kniznica = repo.rebriky().filter { it.skupina != "detsky" }.flatMap { repo.cvikyRebrika(it.id) }.filter { !it.jeDetsky }
        val vlastne = repo.vlastneCviky().mapNotNull { repo.cvikPreVlastny("$PREFIX_VLASTNY${it.id}") }
        _ponuka.value = vlastne + kniznica
    }

    fun nazov(n: String) { _nazov.value = n.take(40) }
    fun styl(s: StylTreningu) { _styl.value = s }

    fun pridaj(cvik: CvikEntity) {
        val vydrz = cvik.meranie == "vydrz"
        val vlastny = cvik.id.startsWith(PREFIX_VLASTNY)
        // Predvolená dávka: stred pásma cviku (vlastný cvik pásmo nemá - 10 opakovaní / 30 s).
        val ciel = when {
            vlastny -> if (vydrz) 30 else 10
            else -> (cvik.cielMin + cvik.cielMax) / 2
        }.coerceAtLeast(1)
        _riadky.value = _riadky.value + RiadokVlastneho(VlastnaPolozka(cvik.id, 3, ciel, pauzaS = Pravidla.PAUZA_SILA_S), cvik)
    }

    fun uprav(i: Int, zmena: (VlastnaPolozka) -> VlastnaPolozka) {
        _riadky.value = _riadky.value.mapIndexed { j, r -> if (j == i) r.copy(polozka = zmena(r.polozka)) else r }
    }

    fun odober(i: Int) { _riadky.value = _riadky.value.filterIndexed { j, _ -> j != i } }

    fun posun(i: Int, o: Int) {
        val l = _riadky.value.toMutableList()
        val j = i + o
        if (j !in l.indices) return
        val x = l[i]; l[i] = l[j]; l[j] = x
        _riadky.value = l
    }

    fun novyCvik(nazov: String, meranie: String, popis: String) {
        viewModelScope.launch {
            val nid = repo.ulozVlastnyCvik(VlastnyCvikEntity(nazov = nazov.trim(), meranie = meranie, popis = popis.trim()))
            obnovPonuku()
            repo.cvikPreVlastny("$PREFIX_VLASTNY$nid")?.let { pridaj(it) }
        }
    }

    fun uloz(hotovo: () -> Unit) {
        viewModelScope.launch {
            repo.ulozVlastnyTrening(id, _nazov.value.trim().ifBlank { "Môj tréning" }, _styl.value, _riadky.value.map { it.polozka })
            hotovo()
        }
    }

    fun zmaz(hotovo: () -> Unit) {
        viewModelScope.launch {
            if (id > 0) repo.zmazVlastnyTrening(id)
            hotovo()
        }
    }
}

/**
 * Editor vlastného tréningu: názov, štýl (klasika / páry / kruh), cviky z knižnice aj vlastné,
 * pri každom série a dávka. Uložený tréning sa spúšťa z Domova (Moje tréningy).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VlastnyTreningObrazovka(id: Long, spat: () -> Unit, model: VlastnyTreningModel = viewModel()) {
    LaunchedEffect(id) { model.nacitaj(id) }
    val nazov by model.nazov.collectAsStateWithLifecycle()
    val styl by model.styl.collectAsStateWithLifecycle()
    val riadky by model.riadky.collectAsStateWithLifecycle()
    val ponuka by model.ponuka.collectAsStateWithLifecycle()
    var vyber by rememberSaveable { mutableStateOf(false) }
    var novy by rememberSaveable { mutableStateOf(false) }
    var mazat by rememberSaveable { mutableStateOf(false) }
    val haptika = rememberHaptika()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = spat) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Späť") }
            Text(if (id > 0) "Upraviť tréning" else "Nový tréning", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (id > 0) {
                IconButton(onClick = { mazat = true }) { Icon(Icons.Rounded.Delete, contentDescription = "Zmazať tréning") }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(
                    value = nazov,
                    onValueChange = model::nazov,
                    label = { Text("Názov") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StylTreningu.entries.forEach { s ->
                        FilterChip(selected = styl == s, onClick = { haptika.tik(); model.styl(s) }, label = { Text(s.nazov) })
                    }
                }
                Text(styl.popis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                NadpisSekcie("Cviky")
            }
            itemsIndexed(riadky) { i, r ->
                RiadokEditora(
                    r = r,
                    kruh = styl == StylTreningu.KRUH,
                    naZmenu = { z -> model.uprav(i, z) },
                    naOdober = { model.odober(i) },
                    naHore = { model.posun(i, -1) },
                    naDole = { model.posun(i, 1) },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vyber = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pridať cvik")
                    }
                    OutlinedButton(onClick = { novy = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text("Vlastný cvik")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        VelkeTlacidlo(text = "Uložiť", onClick = { model.uloz(spat) }, enabled = riadky.isNotEmpty())
    }

    if (vyber) VyberCviku(ponuka, zavri = { vyber = false }, naVyber = { model.pridaj(it); vyber = false })
    if (novy) NovyCvikDialog(zavri = { novy = false }, uloz = { n, m, p -> model.novyCvik(n, m, p); novy = false })
    if (mazat) {
        AlertDialog(
            onDismissRequest = { mazat = false },
            title = { Text("Zmazať tréning?") },
            text = { Text("Šablóna sa zmaže. Odcvičené tréningy v histórii ostanú.") },
            confirmButton = { TextButton(onClick = { model.zmaz(spat) }) { Text("Zmazať") } },
            dismissButton = { TextButton(onClick = { mazat = false }) { Text("Nechať") } },
        )
    }
}

@Composable
private fun RiadokEditora(
    r: RiadokVlastneho,
    kruh: Boolean,
    naZmenu: ((VlastnaPolozka) -> VlastnaPolozka) -> Unit,
    naOdober: () -> Unit,
    naHore: () -> Unit,
    naDole: () -> Unit,
) {
    val p = r.polozka
    val vydrz = r.cvik.meranie == "vydrz"
    val naCas = vydrz || p.naCasS != null
    TonovanaKarta(vnutorneOdsadenie = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Postavicka(
                rebrikId = r.cvik.rebrikId,
                pohyb = r.cvik.pohyb,
                modifier = Modifier.size(44.dp),
                farba = MaterialTheme.colorScheme.onSurface,
                farbaAkcent = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(r.cvik.nazov, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = naHore) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Vyššie") }
            IconButton(onClick = naDole) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Nižšie") }
            IconButton(onClick = naOdober) { Icon(Icons.Rounded.Delete, contentDescription = "Odobrať") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pocitadlo(if (kruh) "kolá" else "série", p.serie, 1) { d -> naZmenu { it.copy(serie = (it.serie + d).coerceIn(1, 10)) } }
            Spacer(Modifier.width(12.dp))
            if (naCas) {
                val s = p.naCasS ?: p.ciel
                Pocitadlo("sekúnd", s, 5) { d ->
                    naZmenu { if (vydrz) it.copy(ciel = (it.ciel + d).coerceIn(5, 600)) else it.copy(naCasS = ((it.naCasS ?: 40) + d).coerceIn(10, 300)) }
                }
            } else {
                Pocitadlo("opakovaní", p.ciel, 1) { d -> naZmenu { it.copy(ciel = (it.ciel + d).coerceIn(1, 200)) } }
            }
        }
        if (!vydrz) {
            Text(
                if (p.naCasS != null) "Na čas · prepnúť na počet" else "Na počet · prepnúť na čas",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { naZmenu { it.copy(naCasS = if (it.naCasS == null) 40 else null) } }
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Pocitadlo(popis: String, hodnota: Int, krok: Int, zmena: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { zmena(-krok) }) { Icon(Icons.Rounded.Remove, contentDescription = "Menej $popis") }
        Text("$hodnota", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = { zmena(krok) }) { Icon(Icons.Rounded.Add, contentDescription = "Viac $popis") }
        Text(popis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Výber cviku s hľadaním - vlastné cviky hore, potom celá knižnica. */
@Composable
private fun VyberCviku(ponuka: List<CvikEntity>, zavri: () -> Unit, naVyber: (CvikEntity) -> Unit) {
    var hladaj by rememberSaveable { mutableStateOf("") }
    val filtrovane = remember(ponuka, hladaj) {
        val h = hladaj.trim().lowercase()
        if (h.isEmpty()) ponuka else ponuka.filter { it.nazov.lowercase().contains(h) }
    }
    AlertDialog(
        onDismissRequest = zavri,
        title = { Text("Pridať cvik") },
        text = {
            Column {
                OutlinedTextField(value = hladaj, onValueChange = { hladaj = it }, label = { Text("Hľadať") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(filtrovane, key = { it.id }) { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable { naVyber(c) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Postavicka(
                                rebrikId = c.rebrikId,
                                pohyb = c.pohyb,
                                modifier = Modifier.size(36.dp),
                                farba = MaterialTheme.colorScheme.onSurface,
                                farbaAkcent = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(c.nazov, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (c.id.startsWith(PREFIX_VLASTNY)) "vlastný cvik" else "${c.rebrikId} · stupeň ${c.uroven}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = zavri) { Text("Zavrieť") } },
    )
}

/** Vlastný cvik, ktorý v knižnici nie je: názov, na počet / na čas, krátky popis (prečíta ho hlas). */
@Composable
private fun NovyCvikDialog(zavri: () -> Unit, uloz: (String, String, String) -> Unit) {
    var nazov by rememberSaveable { mutableStateOf("") }
    var naCas by rememberSaveable { mutableStateOf(false) }
    var popis by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = zavri,
        title = { Text("Vlastný cvik") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nazov, onValueChange = { nazov = it.take(40) }, label = { Text("Názov") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !naCas, onClick = { naCas = false }, label = { Text("Na počet") })
                    FilterChip(selected = naCas, onClick = { naCas = true }, label = { Text("Na čas") })
                }
                OutlinedTextField(
                    value = popis,
                    onValueChange = { popis = it.take(300) },
                    label = { Text("Ako sa robí (každý pokyn na nový riadok)") },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = nazov.isNotBlank(), onClick = { uloz(nazov, if (naCas) "vydrz" else "opakovania", popis) }) { Text("Pridať") }
        },
        dismissButton = { TextButton(onClick = zavri) { Text("Zrušiť") } },
    )
}
