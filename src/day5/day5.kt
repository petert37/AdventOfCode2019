package day5

import kotlinx.coroutines.runBlocking
import util.IntcodeComputer

fun main() = runBlocking<Unit> {
    val input = IntcodeComputer.readProgram("src/day5/input.txt")
    IntcodeComputer(input).run()
}