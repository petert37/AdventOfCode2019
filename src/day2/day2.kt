package day2

import kotlinx.coroutines.runBlocking
import util.IntcodeComputer

fun main() {
    val input = IntcodeComputer.readProgram("src/day2/input.txt")
    val gravityAssistProgram = restoreGravityAssistProgram(input)
    println("Position 0: ${gravityAssistProgram[0]}")
    val pair = findNounAndVerb(input, 19690720)
    println("Part Two: ${100 * pair.first + pair.second}")
}

fun restoreGravityAssistProgram(input: List<Long>): List<Long> = runBlocking {
    val program = input.toMutableList()
    program[1] = 12
    program[2] = 2
    return@runBlocking IntcodeComputer(program).run()
}

fun runProgram(input: List<Long>, noun: Long, verb: Long): List<Long> = runBlocking {
    val program = input.toMutableList()
    program[1] = noun
    program[2] = verb
    return@runBlocking IntcodeComputer(program).run()
}

fun findNounAndVerb(input: List<Long>, output: Long): Pair<Int, Int> {
    for (noun in 0..99)
        for (verb in 0..99)
            if (runProgram(input, noun.toLong(), verb.toLong())[0] == output) return Pair(noun, verb)
    return Pair(-1, -1)
}