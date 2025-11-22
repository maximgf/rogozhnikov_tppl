package cow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

class CowInterpreterTest {

    @Test
    fun shouldIncrementAndDecrementValue() {
        val interpreter = CowInterpreter()
        interpreter.run("MoO MoO MOo")
        assertEquals(1, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldMovePointer() {
        val interpreter = CowInterpreter()
        interpreter.run("moO MoO mOo MoO")
        assertEquals(1, interpreter.getMemoryAt(1))
        assertEquals(1, interpreter.getMemoryAt(0))
        assertEquals(0, interpreter.getPtr())
    }

    @Test
    fun shouldZeroOutCell() {
        val interpreter = CowInterpreter()
        interpreter.run("MoO MoO OOO")
        assertEquals(0, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldHandleLoopCorrectly() {
        val interpreter = CowInterpreter()
        interpreter.run("MoO MoO MOO MOo moo")
        assertEquals(0, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldSkipLoopIfZero() {
        val interpreter = CowInterpreter()
        interpreter.run("MOO MoO moo")
        assertEquals(0, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldThrowErrorOnNegativePointer() {
        val interpreter = CowInterpreter()
        assertFailsWith<RuntimeException>("Runtime Error: Pointer < 0") {
            interpreter.run("mOo")
        }
    }

    @Test
    fun shouldOutputInteger() {
        val outStream = ByteArrayOutputStream()
        val interpreter = CowInterpreter(output = PrintStream(outStream))
        interpreter.run("MoO OOM MoO OOM")
        assertEquals("12", outStream.toString())
    }

    @Test
    fun shouldInputInteger() {
        val inputData = "42\n100\n"
        val inStream = ByteArrayInputStream(inputData.toByteArray(StandardCharsets.UTF_8))
        val interpreter = CowInterpreter(input = inStream)
        interpreter.run("oom moO oom")
        assertEquals(42, interpreter.getMemoryAt(0))
        assertEquals(100, interpreter.getMemoryAt(1))
    }

    @Test
    fun shouldHandleInvalidIntegerInputGracefully() {
        val inputData = "not_a_number\n"
        val inStream = ByteArrayInputStream(inputData.toByteArray(StandardCharsets.UTF_8))
        val interpreter = CowInterpreter(input = inStream)
        interpreter.run("oom")
        assertEquals(0, interpreter.getMemoryAt(0))
    }

    @Test
    fun MooCommandShouldReadCharIfZeroAndPrintCharIfNotZero() {
        val inputData = "A"
        val inStream = ByteArrayInputStream(inputData.toByteArray(StandardCharsets.UTF_8))
        val outStream = ByteArrayOutputStream()

        val interpreter = CowInterpreter(input = inStream, output = PrintStream(outStream))

        interpreter.run("Moo Moo")

        assertEquals(65, interpreter.getMemoryAt(0))
        assertEquals("A", outStream.toString())
    }

    @Test
    fun MMMCommandShouldToggleRegister() {
        val interpreter = CowInterpreter()

        val code = "MoO MMM OOO MMM MMM moO MMM"
        interpreter.run(code)

        assertEquals(1, interpreter.getMemoryAt(0))
        assertEquals(1, interpreter.getMemoryAt(1))
    }

    @Test
    fun mOOCommandExecuteInstructionFromOpcode() {
        val interpreter = CowInterpreter()

        interpreter.run("MoO MoO MoO MoO MoO MoO MoO mOO")

        assertEquals(0, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldIgnoreNonCommandCharacters() {
        val interpreter = CowInterpreter()
        interpreter.run("Hello MoO World MoO !!!")
        assertEquals(2, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldThrowSyntaxErrorOnUnmatchedEndLoop() {
        val interpreter = CowInterpreter()
        assertFailsWith<RuntimeException>("Syntax Error: Unmatched 'moo' (end loop)") {
            interpreter.run("MoO Moo moo")
        }
    }

    @Test
    fun shouldThrowSyntaxErrorOnUnmatchedStartLoop() {
        val interpreter = CowInterpreter()
        assertFailsWith<RuntimeException>("Syntax Error: Unmatched 'MOO' (start loop)") {
            interpreter.run("MoO MOO MoO")
        }
    }
}