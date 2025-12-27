import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*

class SensorAppTest {

    @BeforeTest
    fun setup() {
        isRunning.set(true)
        File(OUTPUT_FILE).delete()
    }

    @AfterTest
    fun cleanup() {
        File(OUTPUT_FILE).delete()
    }

    // --- Тесты парсинга пакетов (Логика + CRC) ---

    @Test
    fun `test parsePacketPort5123 success`() {
        val timeMicros = 1700000000000000L
        val temp = 25.5f
        val press: Short = 1013

        val buffer = ByteBuffer.allocate(15).order(ByteOrder.BIG_ENDIAN)
        buffer.putLong(timeMicros)
        buffer.putFloat(temp)
        buffer.putShort(press)

        // Считаем CRC
        var sum = 0
        val arr = buffer.array()
        for (i in 0 until 14) sum += (arr[i].toInt() and 0xFF)
        buffer.put(sum.toByte())

        buffer.rewind()
        val result = parsePacketPort5123(buffer)

        assertNotNull(result)
        assertTrue(result.contains("Temp=25,50") || result.contains("Temp=25.50"))
        assertTrue(result.contains("Press=1013"))
    }

    @Test
    fun `test parsePacketPort5123 CRC failure`() {
        val buffer = ByteBuffer.allocate(15).order(ByteOrder.BIG_ENDIAN)
        for (i in 0 until 15) buffer.put(1) // Неверная сумма
        buffer.rewind()

        val result = parsePacketPort5123(buffer)
        assertNull(result)
    }

    @Test
    fun `test parsePacketPort5124 success`() {
        val buffer = ByteBuffer.allocate(21).order(ByteOrder.BIG_ENDIAN)
        buffer.putLong(0) // time
        buffer.putInt(10) // x
        buffer.putInt(-20) // y
        buffer.putInt(30) // z

        var sum = 0
        val arr = buffer.array()
        for (i in 0 until 20) sum += (arr[i].toInt() and 0xFF)
        buffer.put(sum.toByte())

        buffer.rewind()
        val result = parsePacketPort5124(buffer)

        assertNotNull(result)
        assertTrue(result.contains("X=10;Y=-20;Z=30"))
    }

    // --- Тесты утилит ---

    @Test
    fun `test formatMicroseconds`() {
        // 0 microseconds -> 1970-01-01 (в UTC)
        val result = formatMicroseconds(0L)
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    // --- Тесты файловой системы ---

    @Test
    fun `test processFileWriting creates header and writes data`() = runTest {
        val channel = Channel<String>()

        // Запускаем запись в отдельной корутине
        val job = launch {
            processFileWriting(channel)
        }

        channel.send("TestLine1")
        channel.send("TestLine2")
        channel.close()
        job.join()

        val lines = File(OUTPUT_FILE).readLines()
        assertEquals(3, lines.size)
        assertEquals("Source;Time;Data_Values...", lines[0])
        assertEquals("TestLine1", lines[1])
        assertEquals("TestLine2", lines[2])
    }
}