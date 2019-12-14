package day5

import util.IntcodeComputer
import java.io.File

fun main() {
    val input = File("src/day5/input.txt").readText().split(",").map { it.toInt() }
    IntcodeComputer(input).run()
}