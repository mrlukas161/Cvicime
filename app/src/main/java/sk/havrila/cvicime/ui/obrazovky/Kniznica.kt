package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.model.Skupina

class KniznicaModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _rebriky = MutableStateFlow<List<RebrikEntity>>(emptyList())
    val rebriky = _rebriky.asStateFlow()

    private val _urovne = MutableStateFlow<Map<String, Int>>(emptyMap())
    val urovne = _urovne.asStateFlow()

    // Tokom, nie jednorazovo: katalóg sa načítava na pozadí a úrovne sa menia po každom tréningu.
    init {
        viewModelScope.launch {
            repo.rebrikyTok().collectLatest { _rebriky.value = it }
        }
        viewModelScope.launch {
            repo.denik.urovneTok().collectLatest { zoznam ->
                _urovne.value = zoznam.associate { it.rebrikId to it.uroven }
            }
        }
    }
}

@Composable
fun KniznicaObrazovka(naRebrik: (String) -> Unit, model: KniznicaModel = viewModel()) {
    val rebriky by model.rebriky.collectAsStateWithLifecycle()
    val urovne by model.urovne.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(20.dp)) {
                Text("Cviky", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Každý cvik je rebrík stupňov. Postupuje sa na ťažšiu variáciu, nie na viac " +
                        "opakovaní donekonečna — to je celý princíp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(rebriky) { r ->
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clickable { naRebrik(r.id) },
                colors = CardDefaults.cardColors(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(r.nazov, style = MaterialTheme.typography.titleLarge)
                    val u = urovne[r.id]
                    Text(
                        buildString {
                            append(Skupina.z(r.skupina).nazov)
                            if (u != null) append(" · si na stupni $u")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

class RebrikModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _rebrik = MutableStateFlow<RebrikEntity?>(null)
    val rebrik = _rebrik.asStateFlow()

    private val _cviky = MutableStateFlow<List<CvikEntity>>(emptyList())
    val cviky = _cviky.asStateFlow()

    private val _uroven = MutableStateFlow(1)
    val uroven = _uroven.asStateFlow()

    fun nacitaj(rebrikId: String) {
        viewModelScope.launch {
            _rebrik.value = repo.katalog.rebrik(rebrikId)
            _cviky.value = repo.cvikyRebrika(rebrikId)
            _uroven.value = repo.urovenVRebriku(rebrikId)
        }
    }
}

@Composable
fun RebrikObrazovka(rebrikId: String, spat: () -> Unit, model: RebrikModel = viewModel()) {
    val rebrik by model.rebrik.collectAsStateWithLifecycle()
    val cviky by model.cviky.collectAsStateWithLifecycle()
    val uroven by model.uroven.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(rebrikId) { model.nacitaj(rebrikId) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(20.dp)) {
                TextButton(onClick = spat) { Text("← Späť") }
                Text(rebrik?.nazov ?: "", style = MaterialTheme.typography.headlineLarge)
                rebrik?.popis?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(cviky) { c ->
            val aktualny = c.uroven == uroven
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                colors = if (aktualny) {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    CardDefaults.cardColors()
                },
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "${c.uroven}. ${c.nazov}" + if (aktualny) "   ← tu si" else "",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    val jednotka = if (c.meranie == "vydrz") "s" else "opakovaní"
                    Text(
                        "Cieľ ${c.cielMin}–${c.cielMax} $jednotka",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    c.cue.split("|").filter { it.isNotBlank() }.forEach {
                        Spacer(Modifier.height(4.dp))
                        Text("• $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    c.chyby.split("|").filter { it.isNotBlank() }.forEach {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "✗ $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
