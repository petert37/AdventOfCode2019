package day5

import kotlinx.coroutines.runBlocking
import util.IntcodeComputer
import java.io.File

fun main() = runBlocking<Unit> {
    val input = File("src/day5/input.txt").readText().split(",").map { it.toInt() }
    IntcodeComputer(input).run()
}