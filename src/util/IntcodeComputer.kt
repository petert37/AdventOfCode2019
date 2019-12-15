package util

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.io.File
import kotlin.math.pow

private typealias Parameter = Pair<ParameterMode, Int>
private typealias Program = MutableList<Int>

enum class Opcode(val opcode: Int, val parameterCount: Int) {
    Add(1, 3),
    Multiply(2, 3),
    Input(3, 1),
    Output(4, 1),
    JumpIfTrue(5, 2),
    JumpIfFalse(6, 2),
    LessThan(7, 3),
    Equals(8, 3),
    Halt(99, 0);

    companion object {
        private val map = values().associateBy(Opcode::opcode)
        fun fromOpcode(opcode: Int) = map[opcode]
    }
}

enum class ParameterMode(val code: Int) {
    Position(0),
    Immediate(1);

    companion object {
        private val map = values().associateBy(ParameterMode::code)
        fun fromCode(code: Int) = map[code]
    }
}

open class IntcodeComputer(protected val program: List<Int>, protected val debug: Boolean = false) {

    suspend fun run(): List<Int> {
        val program = this.program.toMutableList()
        var pc = 0
        while (true) {
            val opcode = Opcode.fromOpcode(program[pc] % 100) ?: throw Exception("Unknown opcode")
            if (opcode == Opcode.Halt) {
                beforeHalt()
                break
            }
            val params = program.subList(pc + 1, pc + opcode.parameterCount + 1).mapIndexed { i, value ->
                val mode = ParameterMode.fromCode(program[pc] / 10.0.pow(i + 2).toInt() % 10)
                    ?: throw Exception("Unknown parameter mode")
                mode to value
            }
            pc = executeInstruction(pc, program, opcode, params)
        }
        return program.toList()
    }

    suspend fun executeInstruction(pc: Int, program: Program, opcode: Opcode, params: List<Parameter>): Int {

        if (debug) {
            println("INSTRUCTION: ${opcode.name}($params)")
        }

        when (opcode) {
            Opcode.Add -> program.setParameterValue(
                params[2],
                program.getParameterValue(params[0]) + program.getParameterValue(params[1])
            )
            Opcode.Multiply -> program.setParameterValue(
                params[2],
                program.getParameterValue(params[0]) * program.getParameterValue(params[1])
            )
            Opcode.Input -> program.setParameterValue(params[0], readInput())
            Opcode.Output -> writeOutput(program.getParameterValue(params[0]))
            Opcode.JumpIfTrue -> {
                if (program.getParameterValue(params[0]) != 0)
                    return program.getParameterValue(params[1])
            }
            Opcode.JumpIfFalse -> {
                if (program.getParameterValue(params[0]) == 0)
                    return program.getParameterValue(params[1])
            }
            Opcode.LessThan -> {
                if (program.getParameterValue(params[0]) < program.getParameterValue(params[1]))
                    program.setParameterValue(params[2], 1)
                else
                    program.setParameterValue(params[2], 0)
            }
            Opcode.Equals -> {
                if (program.getParameterValue(params[0]) == program.getParameterValue(params[1]))
                    program.setParameterValue(params[2], 1)
                else
                    program.setParameterValue(params[2], 0)
            }
            Opcode.Halt -> throw Error("Cannot execute Halt instruction")
        }
        return pc + opcode.parameterCount + 1
    }

    protected open suspend fun readInput(): Int {
        var input: String? = null
        while (input == null) {
            print("INPUT: ")
            input = readLine()
            if (input == null) {
                println("You must provide an input!")
            } else {
                try {
                    return input.toInt()
                } catch (e: Exception) {
                    println("You must provide a number!")
                    input = null
                }
            }
        }
        throw Exception("Should not happen")
    }

    protected open suspend fun writeOutput(output: Int) {
        println("OUTPUT: $output")
    }

    protected open fun beforeHalt() {
    }

    companion object {
        fun readProgram(path: String) = File(path).readText().split(",").map { it.toInt() }
    }

}

private fun Program.getParameterValue(parameter: Parameter) = when (parameter.first) {
    ParameterMode.Immediate -> parameter.second
    ParameterMode.Position -> this[parameter.second]
}

private fun Program.setParameterValue(parameter: Parameter, value: Int) {
    if (parameter.first == ParameterMode.Immediate)
        throw Exception("Write parameters cannot be in immediate mode")
    this[parameter.second] = value
}

class AsyncIntcodeComputer(
    program: List<Int>,
    private val input: ReceiveChannel<Int>,
    private val output: SendChannel<Int>,
    debug: Boolean = false
) : IntcodeComputer(program, debug) {

    override suspend fun readInput(): Int {
        val value = input.receive()
        if (debug) {
            println("Read input: $value")
        }
        return value
    }

    override suspend fun writeOutput(output: Int) {
        if (debug) {
            println("Writing output: $output")
        }
        this.output.send(output)
    }

    override fun beforeHalt() {
        output.close()
    }
}