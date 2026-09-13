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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class Player(
    val id: Int,
    val name: String,
    val score: Double = 0.0
)

data class Change(
    val playerId: Int,
    val amount: Double
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PunktatorApp()
        }
    }
}

/* =========================
   KOLORY
   ========================= */

private val BackgroundColor = Color(0xFF0F0F0F)
private val CardColor = Color(0xFF1A1A1A)
private val CardColorLight = Color(0xFF222222)
private val GoldColor = Color(0xFFD6B36A)
private val GoldDarkColor = Color(0xFFB89450)
private val TextPrimary = Color(0xFFF5F5F5)
private val TextSecondary = Color(0xFFBDBDBD)
private val BorderColor = Color(0xFF3A3A3A)
private val NegativeColor = Color(0xFFE57373)

private val PunktatorDarkColors = darkColorScheme(
    primary = GoldColor,
    onPrimary = Color(0xFF17120A),

    primaryContainer = Color(0xFF4A3A1E),
    onPrimaryContainer = Color(0xFFFFEBC1),

    secondary = GoldColor,
    onSecondary = Color(0xFF17120A),

    secondaryContainer = Color(0xFF3B3120),
    onSecondaryContainer = Color(0xFFFFEBC1),

    background = BackgroundColor,
    onBackground = TextPrimary,

    surface = CardColor,
    onSurface = TextPrimary,

    surfaceVariant = CardColorLight,
    onSurfaceVariant = TextSecondary,

    outline = BorderColor,

    error = NegativeColor,
    onError = Color.Black
)

/* =========================
   GŁÓWNA APLIKACJA
   ========================= */

@Composable
fun PunktatorApp() {

    var screen by remember {
        mutableStateOf("setup")
    }

    var players by remember {
        mutableStateOf(listOf<Player>())
    }

    var nextId by remember {
        mutableIntStateOf(1)
    }

    var history by remember {
        mutableStateOf(listOf<Change>())
    }

    var customDialog by remember {
        mutableStateOf<Int?>(null)
    }

    MaterialTheme(
        colorScheme = PunktatorDarkColors
    ) {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundColor
        ) {

            when (screen) {

                "setup" -> {

                    SetupScreen(
                        players = players,

                        onAdd = {

                            players = players + Player(
                                id = nextId,
                                name = "Gracz $nextId"
                            )

                            nextId++
                        },

                        onNameChange = { id, name ->

                            players = players.map { player ->

                                if (player.id == id) {
                                    player.copy(name = name)
                                } else {
                                    player
                                }
                            }
                        },

                        onRemove = { id ->

                            players = players.filterNot {
                                it.id == id
                            }
                        },

                        onStart = {

                            history = emptyList()
                            screen = "game"
                        }
                    )
                }

                "game" -> {

                    GameScreen(
                        players = players,
                        canUndo = history.isNotEmpty(),

                        onAddPoints = { id, amount ->

                            players = players.map { player ->

                                if (player.id == id) {

                                    player.copy(
                                        score = player.score + amount
                                    )

                                } else {
                                    player
                                }
                            }

                            history = history + Change(
                                playerId = id,
                                amount = amount
                            )
                        },

                        onUndo = {

                            val last = history.lastOrNull()

                            if (last != null) {

                                players = players.map { player ->

                                    if (player.id == last.playerId) {

                                        player.copy(
                                            score =
                                                player.score - last.amount
                                        )

                                    } else {
                                        player
                                    }
                                }

                                history = history.dropLast(1)
                            }
                        },

                        onCustom = { playerId ->

                            customDialog = playerId
                        },

                        onFinish = {

                            screen = "results"
                        }
                    )
                }

                "results" -> {

                    ResultsScreen(
                        players = players,

                        onNewGame = {

                            players = players.map { player ->
                                player.copy(score = 0.0)
                            }

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
            }

            customDialog?.let { playerId ->

                CustomPointsDialog(

                    onDismiss = {
                        customDialog = null
                    },

                    onConfirm = { amount ->

                        players = players.map { player ->

                            if (player.id == playerId) {

                                player.copy(
                                    score =
                                        player.score + amount
                                )

                            } else {
                                player
                            }
                        }

                        history = history + Change(
                            playerId = playerId,
                            amount = amount
                        )

                        customDialog = null
                    }
                )
            }
        }
    }
}

/* =========================
   EKRAN NOWEJ GRY
   ========================= */

@Composable
fun SetupScreen(
    players: List<Player>,
    onAdd: () -> Unit,
    onNameChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onStart: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {

        Text(
            text = "Punktator",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Nowa gra",
            fontSize = 20.sp,
            color = TextSecondary,

            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 18.dp
            )
        )

        LazyColumn(
            modifier = Modifier.weight(1f),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            itemsIndexed(
                items = players,
                key = { _, player -> player.id }
            ) { _, player ->

                OutlinedTextField(
                    value = player.name,

                    onValueChange = { name ->

                        onNameChange(
                            player.id,
                            name
                        )
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true,

                    label = {
                        Text("Nazwa gracza")
                    },

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = GoldColor,
                        unfocusedBorderColor = BorderColor,

                        focusedLabelColor = GoldColor,
                        unfocusedLabelColor = TextSecondary,

                        cursorColor = GoldColor,

                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,

                        focusedContainerColor = CardColor,
                        unfocusedContainerColor = CardColor
                    ),

                    trailingIcon = {

                        IconButton(
                            onClick = {
                                onRemove(player.id)
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Usuń",
                                tint = TextSecondary
                            )
                        }
                    }
                )
            }
        }

        OutlinedButton(
            onClick = onAdd,

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = GoldColor
            )
        ) {

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "DODAJ GRACZA",
                fontSize = 16.sp
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Button(
            onClick = onStart,

            enabled =
                players.isNotEmpty() &&
                        players.all {
                            it.name.isNotBlank()
                        },

            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = GoldColor,
                contentColor = Color(0xFF17120A),
                disabledContainerColor = Color(0xFF3A3A3A),
                disabledContentColor = Color(0xFF777777)
            )
        ) {

            Text(
                text = "START",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* =========================
   EKRAN GRY
   ========================= */

@Composable
fun GameScreen(
    players: List<Player>,
    canUndo: Boolean,
    onAddPoints: (Int, Double) -> Unit,
    onUndo: () -> Unit,
    onCustom: (Int) -> Unit,
    onFinish: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Gra",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Button(
                onClick = onFinish,

                modifier = Modifier.height(48.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldColor,
                    contentColor = Color(0xFF17120A)
                )
            ) {

                Text(
                    text = "ZAKOŃCZ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            itemsIndexed(
                items = players,
                key = { _, player -> player.id }
            ) { _, player ->

                Card(
                    shape = RoundedCornerShape(16.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),

                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = player.name,
                                fontSize = 21.sp,
                                fontWeight =
                                    FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Text(
                                text =
                                    formatScore(
                                        player.score
                                    ),

                                fontSize = 26.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = GoldColor
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            PointButton(
                                label = "+0,5",
                                amount = 0.5,
                                id = player.id,
                                onAdd = onAddPoints
                            )

                            PointButton(
                                label = "+1",
                                amount = 1.0,
                                id = player.id,
                                onAdd = onAddPoints
                            )

                            PointButton(
                                label = "+2",
                                amount = 2.0,
                                id = player.id,
                                onAdd = onAddPoints
                            )

                            PointButton(
                                label = "+5",
                                amount = 5.0,
                                id = player.id,
                                onAdd = onAddPoints
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            PointButton(
                                label = "-0,5",
                                amount = -0.5,
                                id = player.id,
                                onAdd = onAddPoints,
                                negative = true
                            )

                            PointButton(
                                label = "-1",
                                amount = -1.0,
                                id = player.id,
                                onAdd = onAddPoints,
                                negative = true
                            )

                            OutlinedButton(
                                onClick = {
                                    onCustom(player.id)
                                },

                                modifier =
                                    Modifier.weight(1f),

                                colors =
                                    ButtonDefaults
                                        .outlinedButtonColors(
                                            contentColor =
                                                GoldColor
                                        )
                            ) {

                                Text("Własna")
                            }
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = onUndo,
            enabled = canUndo,

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSecondary,
                disabledContentColor = Color(0xFF555555)
            )
        ) {

            Icon(
                imageVector = Icons.Default.Undo,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "COFNIJ OSTATNIE PUNKTY",
                fontSize = 15.sp
            )
        }
    }
}

/* =========================
   PRZYCISK PUNKTÓW
   ========================= */

@Composable
fun RowScope.PointButton(
    label: String,
    amount: Double,
    id: Int,
    onAdd: (Int, Double) -> Unit,
    negative: Boolean = false
) {

    Button(
        onClick = {
            onAdd(id, amount)
        },

        modifier = Modifier.weight(1f),

        contentPadding = PaddingValues(
            horizontal = 4.dp
        ),

        colors = ButtonDefaults.buttonColors(

            containerColor =
                if (negative) {
                    Color(0xFF352020)
                } else {
                    Color(0xFF3A301F)
                },

            contentColor =
                if (negative) {
                    Color(0xFFFF9E9E)
                } else {
                    GoldColor
                }
        )
    ) {

        Text(
            text = label,
            fontWeight = FontWeight.Bold
        )
    }
}

/* =========================
   DIALOG WŁASNYCH PUNKTÓW
   ========================= */

@Composable
fun CustomPointsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {

    var text by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        containerColor = CardColor,

        titleContentColor = TextPrimary,

        textContentColor = TextSecondary,

        title = {
            Text("Własna liczba punktów")
        },

        text = {

            OutlinedTextField(
                value = text,

                onValueChange = {
                    text = it
                },

                singleLine = true,

                label = {
                    Text("Np. 3,5 lub -2")
                },

                colors = OutlinedTextFieldDefaults.colors(

                    focusedBorderColor = GoldColor,
                    unfocusedBorderColor = BorderColor,

                    focusedLabelColor = GoldColor,
                    unfocusedLabelColor = TextSecondary,

                    cursorColor = GoldColor,

                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,

                    focusedContainerColor = BackgroundColor,
                    unfocusedContainerColor = BackgroundColor
                )
            )
        },

        confirmButton = {

            TextButton(
                onClick = {

                    val amount =
                        text
                            .replace(',', '.')
                            .toDoubleOrNull()

                    if (amount != null) {
                        onConfirm(amount)
                    }
                }
            ) {

                Text(
                    text = "DODAJ",
                    color = GoldColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "ANULUJ",
                    color = TextSecondary
                )
            }
        }
    )
}

/* =========================
   EKRAN WYNIKÓW
   ========================= */

@Composable
fun ResultsScreen(
    players: List<Player>,
    onNewGame: () -> Unit,
    onSetup: () -> Unit
) {

    val sortedPlayers =
        players.sortedByDescending {
            it.score
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {

        Text(
            text = "Wyniki",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Ranking końcowy",
            fontSize = 18.sp,
            color = TextSecondary,

            modifier = Modifier.padding(
                bottom = 18.dp
            )
        )

        LazyColumn(
            modifier = Modifier.weight(1f),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            itemsIndexed(
                items = sortedPlayers
            ) { index, player ->

                val place = if (
                    index > 0 &&
                    abs(
                        player.score -
                                sortedPlayers[index - 1].score
                    ) < 0.0001
                ) {

                    index

                } else {

                    index + 1
                }

                Card(
                    shape = RoundedCornerShape(16.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),

                    modifier = Modifier.fillMaxWidth()
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                "$place. ${player.name}",

                            fontSize = 20.sp,

                            fontWeight =
                                FontWeight.SemiBold,

                            color = TextPrimary
                        )

                        Text(
                            text =
                                formatScore(
                                    player.score
                                ),

                            fontSize = 23.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color = GoldColor
                        )
                    }
                }
            }
        }

        Button(
            onClick = onNewGame,

            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = GoldColor,
                contentColor = Color(0xFF17120A)
            )
        ) {

            Text(
                text = "NOWA GRA",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = onSetup,

            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),

            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = GoldColor
            )
        ) {

            Text("ZMIENIĆ GRACZY")
        }
    }
}

/* =========================
   FORMATOWANIE PUNKTÓW
   ========================= */

fun formatScore(value: Double): String {

    val rounded =
        round(value * 2.0) / 2.0

    return if (rounded % 1.0 == 0.0) {

        rounded.toInt().toString()

    } else {

        String
            .format(
                Locale.US,
                "%.1f",
                rounded
            )
            .replace('.', ',')
    }
}
