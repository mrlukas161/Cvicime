package sk.havrila.cvicime.ui.obrazovky

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.havrila.cvicime.data.Repozitar
import sk.havrila.cvicime.data.katalog.CvikEntity
import sk.havrila.cvicime.data.katalog.RebrikEntity
import sk.havrila.cvicime.data.model.Skupina

data class RebrikSCvikmi(val rebrik: RebrikEntity, val cviky: List<CvikEntity>)

class TestModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repozitar.ziskaj(app)

    private val _rebriky = MutableStateFlow<List<RebrikSCvikmi>>(emptyList())
    val rebriky = _rebriky.asStateFlow()

    init {
        viewModelScope.launch {
            val zaujimave = setOf(
                Skupina.TLAK, Skupina.TAH, Skupina.NOHY, Skupina.STRED, Skupina.ZRUCNOST,
            )
            _rebriky.value = repo.rebriky()
                .filter { Skupina.z(it.skupina) in zaujimave }
                .map { r -> RebrikSCvikmi(r, repo.cvikyRebrika(r.id).filter { !it.jeDetsky }) }
                .filter { it.cviky.isNotEmpty() }
        }
    }

    fun uloz(vyber: Map<String, Int>, hotovo: () -> Unit) {
        viewModelScope.launch {
            vyber.forEach { (rebrikId, uroven) ->
                repo.nastavUroven(rebrikId, uroven)
                repo.cvikyRebrika(rebrikId).firstOrNull { it.uroven == uroven }?.let { cvik ->
                    repo.zapisTest(cvik, cvik.cielMin, jeVstupny = true)
                }
            }
            val profil = repo.profil()
            repo.ulozProfil(profil.copy(vstupnyTestHotovy = true))
            hotovo()
        }
    }
}

/**
 * Vstupný test nezisťuje maximálky vzorcom - pýta sa priamo, ktorý najťažší stupeň
 * používateľ zvládne. Presne tak to robí tréner a nevyžaduje si to prepočty,
 * ktoré by aj tak boli vymyslené (koeficienty obtiažnosti cvikov nemáme z čoho vziať).
 */
@Composable
fun TestObrazovka(hotovo: () -> Unit, model: TestModel = viewModel()) {
    val rebriky by model.rebriky.collectAsStateWithLifecycle()
    val vyber = remember { mutableStateMapOf<String, Int>() }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp)) {
            Text("Vstupný test", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Pri každom cviku vyber najťažší stupeň, ktorý zvládneš aspoň trikrát " +
                    "s poriadnou technikou. Ak ani prvý — nevadí, nevyber nič, začneme tam.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rebriky) { polozka ->
                Card(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(polozka.rebrik.nazov, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        polozka.cviky.forEach { cvik ->
                            val zvoleny = vyber[polozka.rebrik.id] == cvik.uroven
                            Text(
                                "${cvik.uroven}. ${cvik.nazov}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (zvoleny) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (zvoleny) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable {
                                        if (zvoleny) {
                                            vyber.remove(polozka.rebrik.id)
                                        } else {
                                            vyber[polozka.rebrik.id] = cvik.uroven
                                        }
                                    }
                                    .padding(10.dp),
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { model.uloz(vyber.toMap(), hotovo) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(60.dp),
        ) {
            Text("Hotovo, poďme cvičiť", style = MaterialTheme.typography.titleLarge)
        }
    }
}
