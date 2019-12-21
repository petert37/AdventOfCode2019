package util

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.io.File
import kotlin.math.pow

private typealias Parameter = Pair<ParameterMode, Long>
private typealias Program = MutableList<Long>

enum class Opcode(val opcode: Int, val parameterCount: Int) {
    Add(1, 3),
    Multiply(2, 3),
    Input(3, 1),
    Output(4, 1),
    JumpIfTrue(5, 2),
    JumpIfFalse(6, 2),
    LessThan(7, 3),
    Equals(8, 3),
    RelativeBaseOffset(9, 1),
    Halt(99, 0);

    companion object {
        private val map = values().associateBy(Opcode::opcode)
        fun fromOpcode(opcode: Int) = map[opcode]
    }
}

enum class ParameterMode(val code: Int) {
    Position(0),
    Immediate(1),
    Relative(2);

    companion object {
        private val map = values().associateBy(ParameterMode::code)
        fun fromCode(code: Int) = map[code]
    }
}

open class IntcodeComputer(protected val program: List<Long>, protected val debug: Boolean = false) {

    protected var pc = 0
    protected var relativeBase = 0L
    protected var activeProgram = program.toMutableList()

    suspend fun run(): List<Long> {
        activeProgram = program.toMutableList()
        pc = 0
        relativeBase = 0
        while (true) {
            val opcode = Opcode.fromOpcode((activeProgram[pc] % 100).toInt()) ?: throw Exception("Unknown opcode")
            if (opcode == Opcode.Halt) {
                beforeHalt()
                break
            }
            val params = activeProgram.subList(pc + 1, pc + opcode.parameterCount + 1).mapIndexed { i, value ->
                val mode = ParameterMode.fromCode((activeProgram[pc] / 10.0.pow(i + 2).toLong() % 10).toInt())
                    ?: throw Exception("Unknown parameter mode")
                mode to value
            }
            pc = executeInstruction(opcode, params)
        }
        return activeProgram.toList()
    }

    suspend fun executeInstruction(opcode: Opcode, params: List<Parameter>): Int {

        if (debug) {
            println("INSTRUCTION: ${opcode.name}($params)")
        }

        when (opcode) {
            Opcode.Add -> setParameterValue(
                params[2], getParameterValue(params[0]) + getParameterValue(params[1])
            )
            Opcode.Multiply -> setParameterValue(
                params[2], getParameterValue(params[0]) * getParameterValue(params[1])
            )
            Opcode.Input -> setParameterValue(params[0], readInput())
            Opcode.Output -> writeOutput(getParameterValue(params[0]))
            Opcode.JumpIfTrue -> {
                if (getParameterValue(params[0]) != 0L)
                    return getParameterValue(params[1]).toInt()
            }
            Opcode.JumpIfFalse -> {
                if (getParameterValue(params[0]) == 0L)
                    return getParameterValue(params[1]).toInt()
            }
            Opcode.LessThan -> {
                if (getParameterValue(params[0]) < getParameterValue(params[1]))
                    setParameterValue(params[2], 1)
                else
                    setParameterValue(params[2], 0)
            }
            Opcode.Equals -> {
                if (getParameterValue(params[0]) == getParameterValue(params[1]))
                    setParameterValue(params[2], 1)
                else
                    setParameterValue(params[2], 0)
            }
            Opcode.RelativeBaseOffset -> {
                relativeBase += getParameterValue(params[0])
            }
            Opcode.Halt -> throw Error("Cannot execute Halt instruction")
        }
        return pc + opcode.parameterCount + 1
    }

    protected open suspend fun readInput(): Long {
        var input: String? = null
        while (input == null) {
            print("INPUT: ")
            input = readLine()
            if (input == null) {
                println("You must provide an input!")
            } else {
                try {
                    return input.toLong()
                } catch (e: Exception) {
                    println("You must provide a number!")
                    input = null
                }
            }
        }
        throw Exception("Should not happen")
    }

    protected open suspend fun writeOutput(output: Long) {
        println("OUTPUT: $output")
    }

    protected open fun beforeHalt() {
    }

    private fun getParameterValue(parameter: Parameter) = when (parameter.first) {
        ParameterMode.Immediate -> parameter.second
        ParameterMode.Position -> getProgramPosition(parameter.second)
        ParameterMode.Relative -> getProgramPosition(relativeBase + parameter.second)
    }

    private fun setParameterValue(parameter: Parameter, value: Long) = when (parameter.first) {
        ParameterMode.Immediate -> throw Exception("Write parameters cannot be in immediate mode")
        ParameterMode.Position -> setProgramPosition(parameter.second, value)
        ParameterMode.Relative -> setProgramPosition(relativeBase + parameter.second, value)
    }

    private fun getProgramPosition(index: Long): Long {
        if (index > Int.MAX_VALUE) throw Exception("Index too big")
        expandActiveProgram((index + 1).toInt())
        return activeProgram[index.toInt()]
    }

    private fun setProgramPosition(index: Long, value: Long) {
        if (index > Int.MAX_VALUE) throw Exception("Index too big")
        expandActiveProgram((index + 1).toInt())
        activeProgram[index.toInt()] = value
    }

    private fun expandActiveProgram(size: Int) {
        if (activeProgram.size >= size) return
        activeProgram.addAll(List(size - activeProgram.size) { 0L })
    }

    companion object {
        fun readProgram(path: String) = File(path).readText().split(",").map { it.toLong() }
    }

}

class AsyncIntcodeComputer(
    program: List<Long>,
    private val input: ReceiveChannel<Long>,
    private val output: SendChannel<Long>,
    debug: Boolean = false,
    private val beforeInput: (suspend () -> Unit)? = null
) : IntcodeComputer(program, debug) {

    override suspend fun readInput(): Long {
        beforeInput?.invoke()
        val value = input.receive()
        if (debug) {
            println("Read input: $value")
        }
        return value
    }

    override suspend fun writeOutput(output: Long) {
        if (debug) {
            println("Writing output: $output")
        }
        this.output.send(output)
    }

    override fun beforeHalt() {
        output.close()
    }
}