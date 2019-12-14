package day2

import util.IntcodeComputer
import java.io.File

fun main() {
    val input = File("src/day2/input.txt").readText().split(",").map { it.toInt() }
    val gravityAssistProgram = restoreGravityAssistProgram(input)
    println("Position 0: ${gravityAssistProgram[0]}")
    val pair = findNounAndVerb(input, 19690720)
    println("Part Two: ${100 * pair.first + pair.second}")
}

fun restoreGravityAssistProgram(input: List<Int>): List<Int> {
    val program = input.toMutableList()
    program[1] = 12
    program[2] = 2
    return IntcodeComputer(program).run()
}

fun runProgram(input: List<Int>, noun: Int, verb: Int): List<Int> {
    val program = input.toMutableList()
    program[1] = noun
    program[2] = verb
    return IntcodeComputer(program).run()
}

fun findNounAndVerb(input: List<Int>, output: Int): Pair<Int, Int> {
    for (noun in 0..99)
        for (verb in 0..99)
            if (runProgram(input, noun, verb)[0] == output) return Pair(noun, verb)
    return Pair(-1, -1)
}