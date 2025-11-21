package cow

import java.io.InputStream
import java.io.PrintStream
import java.util.*

enum class CowCommand(val code: String, val opcode: Int) {
    moo("moo", 0),
    mOo("mOo", 1),
    moO("moO", 2),
    mOO("mOO", 3),
    Moo("Moo", 4),
    MOo("MOo", 5),
    MoO("MoO", 6),
    MOO("MOO", 7),
    OOO("OOO", 8),
    MMM("MMM", 9),
    OOM("OOM", 10),
    oom("oom", 11);

    companion object {
        fun fromString(s: String): CowCommand? = entries.find { it.code == s }

        fun fromOpcode(id: Int): CowCommand? = entries.find { it.opcode == id }
    }
}

class CowInterpreter(
    private val input: InputStream = System.`in`,
    private val output: PrintStream = System.out,
    private val memorySize: Int = 30000
) {
    private val memory = IntArray(memorySize)
    private var ptr = 0
    private val scanner = Scanner(input)

    fun run(sourceCode: String) {
        val instructions = parse(sourceCode)
        if (instructions.isEmpty()) return

        val loopMap = buildLoopMap(instructions)

        var ip = 0
        while (ip < instructions.size) {
            val command = instructions[ip]

            ip = executeCommand(command, ip, loopMap)
        }
    }

    private fun executeCommand(cmd: CowCommand, currentIp: Int, loopMap: Map<Int, Int>): Int {
        when (cmd) {
            CowCommand.MoO -> memory[ptr]++
            CowCommand.MOo -> memory[ptr]--

            CowCommand.moO -> {
                ptr++
                if (ptr >= memorySize) ptr = 0
            }
            CowCommand.mOo -> {
                ptr--
                if (ptr < 0) ptr = memorySize - 1
            }

            CowCommand.moo -> {
                if (memory[ptr] == 0) {
                    return loopMap[currentIp] ?: (currentIp + 1)
                }
            }
            CowCommand.MOO -> {
                if (memory[ptr] != 0) {
                    return loopMap[currentIp] ?: (currentIp + 1)
                }
            }

            CowCommand.OOM -> output.print(memory[ptr])

            CowCommand.oom -> {
                if (scanner.hasNextInt()) {
                    memory[ptr] = scanner.nextInt()
                } else {
                    if(scanner.hasNext()) scanner.next()
                    memory[ptr] = 0
                }
            }

            CowCommand.mOO -> {
                val cmdToRun = CowCommand.fromOpcode(memory[ptr])
                if (cmdToRun != null && cmdToRun != CowCommand.mOO) {
                    executeCommand(cmdToRun, currentIp, loopMap)
                }
            }

            CowCommand.Moo -> {
                if (memory[ptr] == 0) {
                    if (scanner.hasNextInt()) {
                        memory[ptr] = scanner.nextInt()
                    }
                } else {
                    output.print(memory[ptr].toChar())
                }
            }

            CowCommand.OOO -> memory[ptr] = 0

            CowCommand.MMM -> { /* No-op */ }
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
            if (cmd == CowCommand.moo) {
                stack.push(i)
            } else if (cmd == CowCommand.MOO) {
                if (stack.isNotEmpty()) {
                    val start = stack.pop()
                    map[start] = i + 1
                    map[i] = start
                }
            }
        }
        return map
    }

    fun getMemoryAt(index: Int): Int = memory[index]
    fun getPtr(): Int = ptr
}