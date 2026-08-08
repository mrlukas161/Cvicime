package sk.havrila.cvicime.ui.obrazovky

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.havrila.cvicime.teoria.Teoria

/**
 * Teoretická časť: prečo appka robí to, čo robí.
 * Každý článok má štítok, či ide o dokázané zistenie alebo o prax trénerov -
 * appka nesmie tvrdiť viac, než vie.
 */
@Composable
fun TeoriaObrazovka(naClanok: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(20.dp)) {
                Text("Prečo to tak je", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Krátke vysvetlenia pravidiel, na ktorých tréningy stoja. Pri každom je poctivo " +
                        "napísané, či ide o vedecky dokázanú vec, alebo o osvedčenú prax bez štúdií.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(Teoria.clanky) { c ->
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clickable { naClanok(c.id) },
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.emoji, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.titulok, style = MaterialTheme.typography.titleLarge)
                        Text(
                            c.perex,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun ClanokObrazovka(clanokId: String, spat: () -> Unit) {
    val c = Teoria.podla(clanokId) ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        TextButton(onClick = spat) { Text("← Späť") }
        Text("${c.emoji}  ${c.titulok}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        AssistChip(onClick = {}, label = { Text(c.zaklad) })
        Spacer(Modifier.height(16.dp))
        Text(c.telo, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(30.dp))
    }
}
