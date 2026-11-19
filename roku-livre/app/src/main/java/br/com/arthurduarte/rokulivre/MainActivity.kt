package br.com.arthurduarte.rokulivre

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch

private val Purple = Color(0xFF9476FF)
private val Background = Color(0xFF0A0B12)
private val Surface = Color(0xFF181A29)
private val KeyBackground = Color(0xFF282A3E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Purple,
                    background = Background,
                    surface = Surface,
                    onSurface = Color.White,
                    onBackground = Color.White
                )
            ) {
                RokuLivreScreen()
            }
        }
    }
}

@Composable
private fun RokuLivreScreen() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("roku_livre_plus", Context.MODE_PRIVATE)
    }
    val scope = rememberCoroutineScope()
    val devices = remember { mutableStateListOf<RokuDevice>() }
    var selected by remember { mutableStateOf<RokuDevice?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Conecte seu celular e a Roku à mesma rede Wi-Fi.") }
    var manualIp by remember { mutableStateOf("") }
    var showManualIp by remember { mutableStateOf(false) }
    var keyboardText by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<RokuApp>>(emptyList()) }

    fun selectDevice(device: RokuDevice) {
        selected = device
        preferences.edit().putString("last_ip", device.ip).apply()
        status = "Conectado a ${device.name}."
    }

    fun connect(ip: String) {
        if (!RokuNetwork.isLocalIp(ip)) {
            status = "Informe um IPv4 privado válido (ex.: 192.168.1.100)."
            return
        }
        scope.launch {
            busy = true
            try {
                val device = RokuNetwork.deviceInfo(ip.trim())
                if (devices.none { it.ip == device.ip }) devices.add(device)
                selectDevice(device)
                showManualIp = false
            } catch (exception: Exception) {
                status = "Não foi possível conectar: ${exception.message ?: "TV indisponível"}"
            } finally {
                busy = false
            }
        }
    }

    fun search() {
        scope.launch {
            busy = true
            status = "Procurando dispositivos Roku na rede..."
            try {
                val addresses = RokuNetwork.discover(context)
                devices.clear()
                addresses.forEach { ip ->
                    try {
                        devices.add(RokuNetwork.deviceInfo(ip))
                    } catch (_: Exception) {
                        devices.add(RokuDevice(ip, "Roku ($ip)"))
                    }
                }
                status = if (devices.isEmpty()) {
                    "Nenhuma Roku encontrada. Verifique a rede ou conecte pelo IP."
                } else {
                    "${devices.size} dispositivo(s) encontrado(s)."
                }
                if (selected == null && devices.size == 1) selectDevice(devices.first())
            } catch (exception: Exception) {
                status = "Erro na busca: ${exception.message ?: "rede indisponível"}"
            } finally {
                busy = false
            }
        }
    }

    fun send(key: String) {
        val ip = selected?.ip ?: return
        scope.launch {
            try {
                RokuNetwork.press(ip, key)
            } catch (exception: Exception) {
                status = "Comando não enviado: ${exception.message ?: "TV indisponível"}"
            }
        }
    }

    LaunchedEffect(Unit) {
        val previousIp = preferences.getString("last_ip", null)
        if (previousIp != null && RokuNetwork.isLocalIp(previousIp)) {
            try {
                selectDevice(RokuNetwork.deviceInfo(previousIp))
            } catch (_: Exception) {
                status = "Última Roku indisponível. Busque novamente ou informe o IP."
            }
        }
    }

    LaunchedEffect(selected?.ip) {
        val ip = selected?.ip
        apps = emptyList()
        if (ip != null) {
            try {
                apps = RokuNetwork.apps(ip)
            } catch (_: Exception) {
                // Some devices do not expose the installed applications list.
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Roku Livre +",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Controle remoto livre de anúncios",
            color = Color(0xFFB8B9CA),
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = selected?.name ?: "Nenhuma TV conectada",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (selected != null) {
                    Text(
                        text = "${selected!!.ip} ${selected!!.model}",
                        color = Color(0xFFB8B9CA),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = ::search, enabled = !busy) {
                        Text("Encontrar TV")
                    }
                    OutlinedButton(onClick = { showManualIp = !showManualIp }, enabled = !busy) {
                        Text("Informar IP")
                    }
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp))
                if (showManualIp) {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it.take(15) },
                        label = { Text("IP da Roku") },
                        placeholder = { Text("192.168.1.100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { connect(manualIp.trim()) }, enabled = !busy) {
                        Text("Conectar")
                    }
                }
                devices.forEach { device ->
                    OutlinedButton(
                        onClick = { selectDevice(device) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${device.name} • ${device.ip}" +
                                if (selected?.ip == device.ip) " ✓" else ""
                        )
                    }
                }
                Text(
                    text = status,
                    color = Color(0xFFCAC9DA),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        val enabled = selected != null
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteKey("⏻", "Power", enabled, ::send, width = 72)
                    RemoteKey("⌂", "Home", enabled, ::send, width = 72)
                    RemoteKey("⋯", "Info", enabled, ::send, width = 72)
                    RemoteKey("⌕", "Search", enabled, ::send, width = 72)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    RemoteKey("↑", "Up", enabled, ::send)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteKey("←", "Left", enabled, ::send)
                    RemoteKey("OK", "Select", enabled, ::send, prominent = true)
                    RemoteKey("→", "Right", enabled, ::send)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    RemoteKey("↓", "Down", enabled, ::send)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteKey("Voltar", "Back", enabled, ::send, width = 100)
                    RemoteKey("Opções ✱", "Info", enabled, ::send, width = 120)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteKey("«", "Rev", enabled, ::send)
                    RemoteKey("⏯", "Play", enabled, ::send)
                    RemoteKey("»", "Fwd", enabled, ::send)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteKey("−", "VolumeDown", enabled, ::send)
                    RemoteKey("Mudo", "VolumeMute", enabled, ::send)
                    RemoteKey("+", "VolumeUp", enabled, ::send)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteKey("Repetir", "InstantReplay", enabled, ::send, width = 100)
                    RemoteKey("Desligar", "PowerOff", enabled, ::send, width = 100)
                    RemoteKey("Ligar", "PowerOn", enabled, ::send, width = 100)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Escrever na TV", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = keyboardText,
                    onValueChange = { if (it.length <= 160) keyboardText = it },
                    label = { Text("Texto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = enabled
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = enabled && keyboardText.isNotEmpty(),
                        onClick = {
                            val ip = selected?.ip ?: return@Button
                            val typed = keyboardText
                            scope.launch {
                                try {
                                    RokuNetwork.typeText(ip, typed)
                                    keyboardText = ""
                                } catch (exception: Exception) {
                                    status = "Não foi possível enviar o texto: ${exception.message}"
                                }
                            }
                        }
                    ) {
                        Text("Enviar texto")
                    }
                    OutlinedButton(enabled = enabled, onClick = { send("Backspace") }) {
                        Text("Apagar")
                    }
                    OutlinedButton(enabled = enabled, onClick = { send("Enter") }) {
                        Text("Enter")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Entradas da TV", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Disponíveis apenas em Roku TVs compatíveis.",
                    color = Color(0xFFB8B9CA),
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "HDMI 1" to "InputHDMI1",
                        "HDMI 2" to "InputHDMI2",
                        "HDMI 3" to "InputHDMI3",
                        "HDMI 4" to "InputHDMI4",
                        "TV" to "InputTuner",
                        "AV" to "InputAV1"
                    ).forEach { (label, key) ->
                        OutlinedButton(enabled = enabled, onClick = { send(key) }) {
                            Text(label)
                        }
                    }
                }
            }
        }

        if (apps.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Aplicativos instalados", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        apps.forEach { app ->
                            FilledTonalButton(
                                onClick = {
                                    val ip = selected?.ip ?: return@FilledTonalButton
                                    scope.launch {
                                        try {
                                            RokuNetwork.launch(ip, app)
                                        } catch (exception: Exception) {
                                            status = "Não foi possível abrir ${app.name}: ${exception.message}"
                                        }
                                    }
                                }
                            ) {
                                Text(app.name)
                            }
                        }
                    }
                }
            }
        }

        Text(
            "Roku Livre + • Uso local • Sem anúncios e sem rastreamento",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8E8FA4),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RemoteKey(
    label: String,
    command: String,
    enabled: Boolean,
    onPress: (String) -> Unit,
    width: Int = 82,
    prominent: Boolean = false
) {
    Button(
        onClick = { onPress(command) },
        enabled = enabled,
        modifier = Modifier.size(width.dp, 62.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (prominent) Purple else KeyBackground,
            contentColor = if (prominent) Background else Color.White
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            fontWeight = if (prominent) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
