package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.TreningEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoriaModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repozitar.ziskaj(app)
    val treningy = repo.denik.posledneTreningyTok(90)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun HistoriaObrazovka(model: HistoriaModel = viewModel()) {
    val treningy by model.treningy.collectAsStateWithLifecycle()
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
        items(treningy) { t -> RiadokTreningu(t, format) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun RiadokTreningu(t: TreningEntity, format: DateTimeFormatter) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
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
        }
    }
}
