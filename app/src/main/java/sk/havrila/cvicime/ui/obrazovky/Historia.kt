package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.SeriaEntity
import sk.havrila.cvicime.data.denik.TreningEntity
import sk.havrila.cvicime.data.model.Blok
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Jeden riadok série v rozbalenom detaile tréningu. */
data class RiadokSerie(
    val blokNazov: String,
    val cvikNazov: String,
    val text: String,
)

class HistoriaModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repozitar.ziskaj(app)

    val treningy = repo.denik.posledneTreningyTok(90)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _detaily = MutableStateFlow<Map<Long, List<RiadokSerie>>>(emptyMap())

    /** Načítané detaily tréningov: id tréningu -> riadky sérií. */
    val detaily: StateFlow<Map<Long, List<RiadokSerie>>> = _detaily.asStateFlow()

    /** Načíta série tréningu spolu s názvami cvikov. Už načítaný detail sa nenačítava znova. */
    fun nacitajDetail(treningId: Long) {
        if (_detaily.value.containsKey(treningId)) return
        viewModelScope.launch {
            val nazvyCvikov = mutableMapOf<String, String>()
            val riadky = repo.denik.serieTreningu(treningId).map { s ->
                val cvikNazov = nazvyCvikov.getOrPut(s.cvikId) {
                    repo.katalog.cvik(s.cvikId)?.nazov ?: s.cvikId
                }
                RiadokSerie(
                    blokNazov = Blok.z(s.blok).nazov,
                    cvikNazov = cvikNazov,
                    text = textSerie(s),
                )
            }
            _detaily.value = _detaily.value + (treningId to riadky)
        }
    }

    private fun textSerie(s: SeriaEntity): String {
        if (s.preskocena) return "preskočená"
        val zaloha = when (s.rir) {
            0 -> "na doraz"
            1 -> "v zálohe 1–2"
            3 -> "v zálohe 3+"
            else -> null
        }
        val hodnota = s.skutocnaHodnota ?: s.planHodnota
        return if (s.meranie == "vydrz") {
            "$hodnota s"
        } else {
            buildString {
                append("$hodnota× (plán ${s.planHodnota})")
                if (zaloha != null) append(" · $zaloha")
            }
        }
    }
}

@Composable
fun HistoriaObrazovka(model: HistoriaModel = viewModel()) {
    val treningy by model.treningy.collectAsStateWithLifecycle()
    val detaily by model.detaily.collectAsStateWithLifecycle()
    var rozbalene by remember { mutableStateOf(setOf<Long>()) }
    val format = DateTimeFormatter.ofPattern("d.M.yyyy")

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(20.dp)) {
                Text("História", style = MaterialTheme.typography.headlineLarge)
                if (treningy.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Zatiaľ nič. Prvý tréning sa tu objaví hneď po dokončení.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(treningy) { t ->
            RiadokTreningu(
                t = t,
                format = format,
                rozbaleny = t.id in rozbalene,
                serie = detaily[t.id],
                naKlik = {
                    rozbalene = if (t.id in rozbalene) {
                        rozbalene - t.id
                    } else {
                        model.nacitajDetail(t.id)
                        rozbalene + t.id
                    }
                },
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun RiadokTreningu(
    t: TreningEntity,
    format: DateTimeFormatter,
    rozbaleny: Boolean,
    serie: List<RiadokSerie>?,
    naKlik: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable(onClick = naKlik)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                LocalDate.ofEpochDay(t.epochDen).format(format),
                style = MaterialTheme.typography.titleLarge,
            )
            val trvanie = t.koniec?.let { (it - t.zaciatok) / 60000 } ?: 0
            Text(
                buildString {
                    append("%02d:%02d".format(t.minutyDna / 60, t.minutyDna % 60))
                    if (trvanie > 0) append(" · $trvanie min")
                    if (t.rodinny) append(" · s deťmi")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rozbaleny) {
                Spacer(Modifier.height(10.dp))
                DetailTreningu(serie)
            }
        }
    }
}

@Composable
private fun DetailTreningu(serie: List<RiadokSerie>?) {
    when {
        serie == null -> Text(
            "Načítavam…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        serie.isEmpty() -> Text(
            "Žiadne zapísané série.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> Column {
            serie.groupBy { it.blokNazov }.forEach { (blokNazov, riadky) ->
                Text(
                    blokNazov,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                riadky.forEach { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 2.dp)
                    ) {
                        Text(
                            r.cvikNazov,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            r.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
