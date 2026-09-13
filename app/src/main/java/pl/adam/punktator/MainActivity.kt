package pl.adam.punktator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class Player(val id: Int, val name: String, val score: Double = 0.0)
data class Change(val playerId: Int, val amount: Double)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PunktatorApp() }
    }
}

@Composable
fun PunktatorApp() {
    var screen by remember { mutableStateOf("setup") }
    var players by remember { mutableStateOf(listOf<Player>()) }
    var nextId by remember { mutableIntStateOf(1) }
    var history by remember { mutableStateOf(listOf<Change>()) }
    var customDialog by remember { mutableStateOf<Int?>(null) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (screen) {
                "setup" -> SetupScreen(
                    players = players,
                    onAdd = {
                        players = players + Player(nextId, "Gracz $nextId")
                        nextId++
                    },
                    onNameChange = { id, name ->
                        players = players.map { if (it.id == id) it.copy(name = name) else it }
                    },
                    onRemove = { id -> players = players.filterNot { it.id == id } },
                    onStart = { history = emptyList(); screen = "game" }
                )
                "game" -> GameScreen(
                    players = players,
                    canUndo = history.isNotEmpty(),
                    onAddPoints = { id, amount ->
                        players = players.map { if (it.id == id) it.copy(score = it.score + amount) else it }
                        history = history + Change(id, amount)
                    },
                    onUndo = {
                        history.lastOrNull()?.let { last ->
                            players = players.map { if (it.id == last.playerId) it.copy(score = it.score - last.amount) else it }
                            history = history.dropLast(1)
                        }
                    },
                    onCustom = { customDialog = it },
                    onFinish = { screen = "results" }
                )
                else -> ResultsScreen(
                    players = players,
                    onNewGame = {
                        players = players.map { it.copy(score = 0.0) }
                        history = emptyList()
                        screen = "game"
                    },
                    onSetup = {
                        players = emptyList()
                        history = emptyList()
                        nextId = 1
                        screen = "setup"
                    }
                )
            }
            customDialog?.let { playerId ->
                CustomPointsDialog(
                    onDismiss = { customDialog = null },
                    onConfirm = { amount ->
                        players = players.map { if (it.id == playerId) it.copy(score = it.score + amount) else it }
                        history = history + Change(playerId, amount)
                        customDialog = null
                    }
                )
            }
        }
    }
}

@Composable
fun SetupScreen(players: List<Player>, onAdd: () -> Unit, onNameChange: (Int, String) -> Unit, onRemove: (Int) -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Punktator", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Nowa gra", fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(players, key = { _, p -> p.id }) { _, player ->
                OutlinedTextField(
                    value = player.name,
                    onValueChange = { onNameChange(player.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nazwa gracza") },
                    trailingIcon = { IconButton(onClick = { onRemove(player.id) }) { Icon(Icons.Default.Delete, "Usuń") } }
                )
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("DODAJ GRACZA", fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onStart, enabled = players.isNotEmpty() && players.all { it.name.isNotBlank() }, modifier = Modifier.fillMaxWidth().height(58.dp)) {
            Text("START", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GameScreen(players: List<Player>, canUndo: Boolean, onAddPoints: (Int, Double) -> Unit, onUndo: () -> Unit, onCustom: (Int) -> Unit, onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gra", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onFinish) { Text("ZAKOŃCZ") }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(players, key = { _, p -> p.id }) { _, p ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                            Text(formatScore(p.score), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PointButton("+0,5", .5, p.id, onAddPoints)
                            PointButton("+1", 1.0, p.id, onAddPoints)
                            PointButton("+2", 2.0, p.id, onAddPoints)
                            PointButton("+5", 5.0, p.id, onAddPoints)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PointButton("-0,5", -.5, p.id, onAddPoints)
                            PointButton("-1", -1.0, p.id, onAddPoints)
                            OutlinedButton(onClick = { onCustom(p.id) }, modifier = Modifier.weight(1f)) { Text("Własna") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Undo, null); Spacer(Modifier.width(8.dp)); Text("COFNIJ OSTATNIE PUNKTY", fontSize = 15.sp)
        }
    }
}

@Composable
fun PointButton(label: String, amount: Double, id: Int, onAdd: (Int, Double) -> Unit) {
    Button(onClick = { onAdd(id, amount) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(label) }
}

@Composable
fun CustomPointsDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Własna liczba punktów") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text("Np. 3,5 lub -2") }) },
        confirmButton = { TextButton(onClick = { text.replace(',', '.').toDoubleOrNull()?.let(onConfirm) }) { Text("DODAJ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANULUJ") } }
    )
}

@Composable
fun ResultsScreen(players: List<Player>, onNewGame: () -> Unit, onSetup: () -> Unit) {
    val sorted = players.sortedByDescending { it.score }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Wyniki", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Ranking końcowy", fontSize = 18.sp, modifier = Modifier.padding(bottom = 18.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(sorted) { index, p ->
                val place = if (index > 0 && abs(p.score - sorted[index - 1].score) < 0.0001) index else index + 1
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("$place. ${p.name}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatScore(p.score), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("NOWA GRA") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSetup, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("ZMIENIĆ GRACZY") }
    }
}

fun formatScore(value: Double): String {
    val rounded = round(value * 2.0) / 2.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format(Locale.US, "%.1f", rounded).replace('.', ',')
}
