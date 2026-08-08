package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.denik.DietaEntity
import sk.havrila.cvicime.deti.Zvierata

class ZbierkaModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _deti = MutableStateFlow<List<DietaEntity>>(emptyList())
    val deti = _deti.asStateFlow()

    /** dietaId -> odomknuté kľúče */
    private val _odomknute = MutableStateFlow<Map<Long, Set<String>>>(emptyMap())
    val odomknute = _odomknute.asStateFlow()

    init {
        viewModelScope.launch {
            repo.detiTok().collectLatest { zoznam ->
                _deti.value = zoznam
                _odomknute.value = zoznam.associate { d ->
                    d.id to repo.denik.zvierataDietata(d.id).map { it.zvieraKluc }.toSet()
                }
            }
        }
    }
}

/**
 * Zbierka zvierat - MAPA TOHO, ČO DIEŤA VIE, nie skóre.
 *
 * Zámerné rozhodnutia z rešerše:
 * - neodomknuté zvieratá sú len otázniky (identita sa neprezrádza vopred),
 * - žiadne počty, percentá ani porovnanie medzi deťmi - každé dieťa má vlastnú stránku,
 * - zviera raz odomknuté sa nikdy nestratí.
 */
@Composable
fun ZbierkaObrazovka(model: ZbierkaModel = viewModel()) {
    val deti by model.deti.collectAsStateWithLifecycle()
    val odomknute by model.odomknute.collectAsStateWithLifecycle()
    var vybraneDieta by remember { mutableStateOf<Long?>(null) }

    val dieta = deti.firstOrNull { it.id == vybraneDieta } ?: deti.firstOrNull()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Moje zvieratá", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))

        if (deti.isEmpty()) {
            Text(
                "Najprv pridaj deti v Nastaveniach. Zvieratá sa odomykajú pri spoločnom cvičení.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        if (deti.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deti.forEach { d ->
                    FilterChip(
                        selected = dieta?.id == d.id,
                        onClick = { vybraneDieta = d.id },
                        label = { Text(d.meno) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        val ma = odomknute[dieta?.id] ?: emptySet()
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Zvierata.vsetky) { z ->
                val odomknuteZviera = z.kluc in ma
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (odomknuteZviera) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (odomknuteZviera) z.emoji else "❔",
                            fontSize = 40.sp,
                        )
                        if (odomknuteZviera) {
                            Text(
                                z.meno,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
