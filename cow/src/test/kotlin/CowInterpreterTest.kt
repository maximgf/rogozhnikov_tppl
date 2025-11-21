package cow

import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

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

        interpreter.run("MoO MoO moo MOo MOO")

        assertEquals(0, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldSkipLoopIfZero() {
        val interpreter = CowInterpreter()

        interpreter.run("moo MOo MOO")

        assertEquals(0, interpreter.getMemoryAt(0))
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
        val inputData = "42 100"
        val inStream = ByteArrayInputStream(inputData.toByteArray())
        val interpreter = CowInterpreter(input = inStream)

        interpreter.run("oom moO oom")

        assertEquals(42, interpreter.getMemoryAt(0))
        assertEquals(100, interpreter.getMemoryAt(1))
    }

    @Test
    fun shouldHandleInvalidInputGracefully() {
        val inputData = "not_a_number"
        val inStream = ByteArrayInputStream(inputData.toByteArray())
        val interpreter = CowInterpreter(input = inStream)

        interpreter.run("oom")

        assertEquals(0, interpreter.getMemoryAt(0))
    }

    @Test
    fun mooCommandIfZeroReadIfNotZeroPrintChar() {
        val inputData = "65"
        val inStream = ByteArrayInputStream(inputData.toByteArray())
        val outStream = ByteArrayOutputStream()

        val interpreter = CowInterpreter(input = inStream, output = PrintStream(outStream))

        interpreter.run("Moo Moo")

        assertEquals(65, interpreter.getMemoryAt(0))
        assertEquals("A", outStream.toString())
    }

    @Test
    fun mOOCommandExecuteInstructionFromOpcode() {
        val interpreter = CowInterpreter()

        interpreter.run("MoO MoO MoO MoO MoO MoO mOO")

        assertEquals(7, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldIgnoreNonCommandCharacters() {
        val interpreter = CowInterpreter()

        interpreter.run("Hello MoO World MoO !!!")

        assertEquals(2, interpreter.getMemoryAt(0))
    }

    @Test
    fun shouldHandlePointerWrapping() {
        val interp1 = CowInterpreter()
        interp1.run("mOo")
        assertEquals(29999, interp1.getPtr())

        val interp2 = CowInterpreter(memorySize = 1)
        interp2.run("moO")
        assertEquals(0, interp2.getPtr())
    }

    @Test
    fun shouldHandleEmptyCode() {
        val interpreter = CowInterpreter()

        interpreter.run("")

        assertEquals(0, interpreter.getPtr())
    }

    @Test
    fun MMMCommandShouldDoNothing() {
        val interpreter = CowInterpreter()

        val code = "MoO MMM MoO"
        interpreter.run(code)

        assertEquals(2, interpreter.getMemoryAt(0))
    }

    @Test
    fun integrationTestSimpleAddition() {
        val interpreter = CowInterpreter()

        val code = "MoO MoO moO MoO MoO MoO moo MOo mOo MoO moO MOO"
        interpreter.run(code)

        assertEquals(5, interpreter.getMemoryAt(0))
        assertEquals(0, interpreter.getMemoryAt(1))
    }
}