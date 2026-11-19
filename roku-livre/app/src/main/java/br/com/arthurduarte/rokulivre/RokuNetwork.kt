package br.com.arthurduarte.rokulivre

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

data class RokuDevice(val ip: String, val name: String, val model: String = "")
data class RokuApp(val id: String, val name: String)

class RokuHttpException(val status: Int, message: String) : IOException(message)

/** Uses only the published Roku ECP API on the device's local IPv4 address, port 8060. */
object RokuNetwork {
    private const val PORT = 8060
    private val locationRegex =
        Regex("""(?im)^location:\s*http://((?:\d{1,3}\.){3}\d{1,3}):8060/?""")

    /** Never send remote commands to a public address or an arbitrary hostname. */
    fun isLocalIp(input: String): Boolean {
        val octets = input.trim().split(".").map { it.toIntOrNull() ?: return false }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        val (a, b) = octets
        return a == 10 || (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) || (a == 169 && b == 254)
    }

    /** SSDP discovery. Some routers isolate clients or block multicast; manual IP is the fallback. */
    suspend fun discover(context: Context): List<String> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("RokuLivreSSDP").apply { setReferenceCounted(false) }
        var acquired = false
        val found = linkedSetOf<String>()
        try {
            try {
                lock.acquire()
                acquired = true
            } catch (_: SecurityException) {
                // UDP may still work even if multicast lock is unavailable.
            }
            DatagramSocket().use { socket ->
                socket.soTimeout = 350
                val message = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: roku:ecp\r\n\r\n"
                ).toByteArray(Charsets.UTF_8)
                val multicast = InetAddress.getByName("239.255.255.250")
                repeat(3) {
                    socket.send(DatagramPacket(message, message.size, multicast, 1900))
                    delay(100)
                }
                val stopAt = SystemClock.elapsedRealtime() + 3_000L
                while (SystemClock.elapsedRealtime() < stopAt) {
                    val bytes = ByteArray(4096)
                    val packet = DatagramPacket(bytes, bytes.size)
                    try {
                        socket.receive(packet)
                        val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val ip = locationRegex.find(response)?.groupValues?.get(1)
                        if (ip != null && isLocalIp(ip)) found.add(ip)
                    } catch (_: SocketTimeoutException) {
                        // Keep listening until the discovery window ends.
                    }
                }
            }
        } finally {
            if (acquired) lock.release()
        }
        found.toList()
    }

    suspend fun deviceInfo(ip: String): RokuDevice = withContext(Dispatchers.IO) {
        val fields = mutableMapOf<String, String>()
        val parser = xmlGet(ip, "query/device-info")
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                val name = parser.name
                if (name in setOf("user-device-name", "friendly-device-name", "model-name")) {
                    fields[name] = parser.nextText().trim()
                }
            }
        }
        RokuDevice(
            ip = ip,
            name = fields["user-device-name"]?.takeIf { it.isNotBlank() }
                ?: fields["friendly-device-name"]?.takeIf { it.isNotBlank() }
                ?: "Roku ($ip)",
            model = fields["model-name"].orEmpty()
        )
    }

    suspend fun apps(ip: String): List<RokuApp> = withContext(Dispatchers.IO) {
        val result = mutableListOf<RokuApp>()
        val parser = xmlGet(ip, "query/apps")
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "app") {
                val id = parser.getAttributeValue(null, "id").orEmpty()
                val name = parser.nextText().trim()
                if (id.matches(Regex("[A-Za-z0-9._-]{1,80}")) && name.isNotEmpty()) {
                    result.add(RokuApp(id, name))
                }
            }
        }
        result
    }

    suspend fun press(ip: String, key: String) {
        require(key.matches(Regex("[A-Za-z0-9]{1,40}"))) { "Tecla inválida" }
        request(ip, "keypress/$key", "POST")
    }

    suspend fun launch(ip: String, app: RokuApp) {
        require(app.id.matches(Regex("[A-Za-z0-9._-]{1,80}")))
        request(ip, "launch/${Uri.encode(app.id)}", "POST")
    }

    suspend fun typeText(ip: String, text: String) {
        require(text.codePointCount(0, text.length) <= 160) { "Texto muito longo" }
        for (codePoint in text.codePoints().toArray()) {
            val character = String(Character.toChars(codePoint))
            request(ip, "keypress/${Uri.encode("Lit_$character")}", "POST")
            delay(55)
        }
    }

    private fun xmlGet(ip: String, path: String): XmlPullParser {
        val xml = request(ip, path, "GET")
        return Xml.newPullParser().apply { setInput(StringReader(xml)) }
    }

    private fun request(ip: String, path: String, method: String): String {
        require(isLocalIp(ip)) { "Use apenas um IP da rede local." }
        val connection = URL("http://$ip:$PORT/$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 1_700
        connection.readTimeout = 2_200
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        try {
            if (method == "POST") {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(0)
                connection.outputStream.use { }
            }
            val status = connection.responseCode
            if (status == 401 || status == 403) {
                throw RokuHttpException(status,
                    "A TV bloqueou o comando. Em Configurações > Sistema > Configurações avançadas > Controle por aplicativos móveis, selecione 'Habilitado'.")
            }
            if (status !in 200..299) {
                throw RokuHttpException(status, "A Roku respondeu HTTP $status.")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
