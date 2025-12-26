import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

// Константы подключения к серверу
const val HOST = "95.163.237.76"
const val PORT_1 = 5123
const val PORT_2 = 5124
const val AUTH_KEY = "isu_pt"
const val CMD_GET = "get"
const val OUTPUT_FILE = "sensor_data.csv"

// Таймауты и задержки
const val SOCKET_TIMEOUT_MS = 5000
const val DELAY_AFTER_AUTH_MS = 200L
const val RECONNECT_DELAY_MS = 1000L

val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())

val isRunning = AtomicBoolean(true)

fun main() = runBlocking {
    // Канал для передачи данных от клиентов к записи в файл
    val dataChannel = Channel<String>(capacity = 5000)

    // Запуск корутины записи в файл
    val fileWriterJob = launch(Dispatchers.IO) {
        processFileWriting(dataChannel)
    }

    // Запуск клиентов для двух портов
    val client1Job = launch(Dispatchers.IO) {
        runPersistentClient(PORT_1, 15) { buffer -> parsePacketPort5123(buffer) }
            .collect { data -> dataChannel.send(data) }
    }

    val client2Job = launch(Dispatchers.IO) {
        runPersistentClient(PORT_2, 21) { buffer -> parsePacketPort5124(buffer) }
            .collect { data -> dataChannel.send(data) }
    }

    // Ожидание ввода для остановки
    readlnOrNull()

    isRunning.set(false)

    // Корректное завершение работы
    client1Job.cancelAndJoin()
    client2Job.cancelAndJoin()
    dataChannel.close()
    fileWriterJob.join()
}

// Запись данных из канала в CSV файл
suspend fun processFileWriting(channel: Channel<String>) {
    val file = File(OUTPUT_FILE)

    FileOutputStream(file, true).use { fos ->
        val writer = fos.bufferedWriter()

        // Запись заголовка при создании нового файла
        if (file.length() == 0L) {
            writer.write("Source;Time;Data_Values...\n")
            writer.flush()
        }

        for (line in channel) {
            writer.write(line)
            writer.newLine()
            writer.flush()
        }
    }
}

// Постоянное подключение к серверу с автопереподключением
fun runPersistentClient(
    port: Int,
    packetSize: Int,
    parser: (ByteBuffer) -> String?
) = flow {

    val buffer = ByteArray(packetSize)
    val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN)

    while (isRunning.get()) {
        try {
            Socket(HOST, port).use { socket ->
                socket.tcpNoDelay = true
                socket.soTimeout = SOCKET_TIMEOUT_MS

                val input = BufferedInputStream(socket.getInputStream())
                val output = BufferedOutputStream(socket.getOutputStream())

                // Аутентификация
                output.write(AUTH_KEY.toByteArray(Charsets.US_ASCII))
                output.flush()

                delay(DELAY_AFTER_AUTH_MS)

                // Очистка буфера от возможных данных после аутентификации
                val availableBytes = input.available()
                if (availableBytes > 0) {
                    input.skip(availableBytes.toLong())
                }

                // Основной цикл чтения данных
                while (isRunning.get()) {
                    output.write(CMD_GET.toByteArray(Charsets.US_ASCII))
                    output.flush()

                    // Чтение пакета полностью
                    var totalRead = 0
                    while (totalRead < packetSize) {
                        val count = input.read(buffer, totalRead, packetSize - totalRead)
                        if (count == -1) throw Exception("EOF received")
                        totalRead += count
                    }

                    byteBuffer.rewind()
                    val result = parser(byteBuffer)

                    if (result != null) {
                        emit(result)
                    } else {
                        throw Exception("CRC Check failed")
                    }
                }
            }
        } catch (e: Exception) {
            if (isRunning.get()) {
                if (e is CancellationException) throw e
                delay(RECONNECT_DELAY_MS)
            }
        }
    }
}

// Парсинг пакета с порта 5123: температура и давление
fun parsePacketPort5123(buffer: ByteBuffer): String? {
    val timeRaw = buffer.long
    val temp = buffer.float
    val pressure = buffer.short
    val checksumReceived = buffer.get()

    // Проверка контрольной суммы
    buffer.rewind()
    var sum = 0
    for (i in 0 until 14) {
        sum += (buffer.get().toInt() and 0xFF)
    }

    if (sum.toByte() != checksumReceived) return null

    val timeString = formatMicroseconds(timeRaw)
    return "$timeString;$PORT_1;Temp=%.2f;Press=%d".format(temp, pressure)
}

// Парсинг пакета с порта 5124: координаты X, Y, Z
fun parsePacketPort5124(buffer: ByteBuffer): String? {
    val timeRaw = buffer.long
    val x = buffer.int
    val y = buffer.int
    val z = buffer.int
    val checksumReceived = buffer.get()

    // Проверка контрольной суммы
    buffer.rewind()
    var sum = 0
    for (i in 0 until 20) {
        sum += (buffer.get().toInt() and 0xFF)
    }

    if (sum.toByte() != checksumReceived) return null

    val timeString = formatMicroseconds(timeRaw)
    return "$timeString;$PORT_2;X=$x;Y=$y;Z=$z"
}

// Преобразование микросекунд в строку формата "yyyy-MM-dd HH:mm:ss"
fun formatMicroseconds(micros: Long): String {
    val seconds = micros / 1_000_000
    val nanos = (micros % 1_000_000) * 1_000
    return timeFormatter.format(Instant.ofEpochSecond(seconds, nanos))
}