package day9

import kotlinx.coroutines.runBlocking
import util.IntcodeComputer

fun main() = runBlocking<Unit> {
    val input = IntcodeComputer.readProgram("src/day9/input.txt")
    IntcodeComputer(input).run()
}