package cow

import java.io.InputStream
import java.io.PrintStream
import java.util.*

enum class CowCommand(val code: String, val opcode: Int) {
    MoO("MoO", 0),
    MOo("MOo", 1),
    moO("moO", 2),
    mOo("mOo", 3),
    MOO("MOO", 4),
    moo("moo", 5),
    OOM("OOM", 6),
    oom("oom", 7),
    MMM("MMM", 8),
    OOO("OOO", 9),
    mOO("mOO", 10),
    Moo("Moo", 11);

    companion object {
        fun fromString(s: String): CowCommand? = entries.find { it.code == s }
        fun fromOpcode(id: Int): CowCommand? = entries.find { it.opcode == id }
    }
}

class CowInterpreter(
    private val input: InputStream = System.`in`,
    private val output: PrintStream = System.out,
    @Suppress("UNUSED_PARAMETER") memorySize: Int = 30000
) {
    private val memory: MutableMap<Int, Int> = mutableMapOf<Int, Int>().withDefault { 0 }
    private var ptr = 0
    private var register: Int? = null

    private val scanner = Scanner(input)

    fun getMemoryAt(index: Int): Int = memory.getValue(index)

    fun getPtr(): Int = ptr

    fun run(sourceCode: String) {
        ptr = 0
        register = null
        memory.clear()

        val instructions = parse(sourceCode)
        if (instructions.isEmpty()) return

        val loopMap = buildLoopMap(instructions)

        var ip = 0
        while (ip < instructions.size) {
            val command = instructions[ip]

            ip = executeCommand(command, ip, loopMap)
        }
    }

    private fun readInt(): Int {
        return if (scanner.hasNextLine()) {
            val line = scanner.nextLine().trim()
            val firstToken = line.split(Regex("\\s+")).firstOrNull()

            firstToken?.toIntOrNull() ?: 0
        } else {
            0
        }
    }

    private fun executeCommand(cmd: CowCommand, currentIp: Int, loopMap: Map<Int, Int>): Int {
        val currentVal = memory.getValue(ptr)

        when (cmd) {
            CowCommand.MoO -> memory[ptr] = currentVal + 1
            CowCommand.MOo -> memory[ptr] = currentVal - 1

            CowCommand.moO -> ptr++
            CowCommand.mOo -> {
                ptr--
                if (ptr < 0) {
                    throw RuntimeException("Runtime Error: Pointer < 0")
                }
            }

            CowCommand.MOO -> {
                if (currentVal == 0) {
                    return loopMap[currentIp] ?: (currentIp + 1)
                }
            }

            CowCommand.moo -> {
                if (currentVal != 0) {
                    return loopMap[currentIp] ?: (currentIp + 1)
                }
            }

            CowCommand.OOM -> output.print(currentVal)
            CowCommand.oom -> memory[ptr] = readInt()

            CowCommand.MMM -> {
                if (register == null) {
                    register = currentVal
                } else {
                    memory[ptr] = register!!
                    register = null
                }
            }

            CowCommand.OOO -> memory[ptr] = 0

            CowCommand.mOO -> {
                val targetOp = currentVal
                if (targetOp != 10 && targetOp != 4 && targetOp != 5) {
                    val cmdToRun = CowCommand.fromOpcode(targetOp)
                    cmdToRun?.let { executeCommand(it, currentIp, loopMap) }
                }
            }

            CowCommand.Moo -> {
                if (currentVal == 0) {
                    val charInt = input.read()
                    memory[ptr] = if (charInt != -1) charInt else 0
                } else {
                    if (currentVal in 0..255) {
                        output.print(currentVal.toChar())
                    }
                }
            }
        }
        return currentIp + 1
    }

    private fun parse(code: String): List<CowCommand> {
        val result = mutableListOf<CowCommand>()
        var i = 0
        while (i + 2 < code.length) {
            val sub = code.substring(i, i + 3)
            val cmd = CowCommand.fromString(sub)
            if (cmd != null) {
                result.add(cmd)
                i += 3
            } else {
                i++
            }
        }
        return result
    }

    private fun buildLoopMap(instructions: List<CowCommand>): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        val stack = Stack<Int>()

        for ((i, cmd) in instructions.withIndex()) {
            if (cmd == CowCommand.MOO) {
                stack.push(i)
            } else if (cmd == CowCommand.moo) {
                if (stack.isEmpty()) {
                    throw RuntimeException("Syntax Error: Unmatched 'moo' (end loop) at instruction #$i. No open loop found.")
                }
                val start = stack.pop()
                map[start] = i + 1
                map[i] = start
            }
        }

        if (stack.isNotEmpty()) {
            throw RuntimeException("Syntax Error: Unmatched 'MOO' (start loop). Loop was opened but never closed.")
        }

        return map
    }
}