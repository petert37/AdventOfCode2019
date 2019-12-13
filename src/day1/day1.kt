package day1

import java.io.File
import kotlin.math.floor
import kotlin.math.max

fun main() {
    val input = File("src/day1/input.txt").readLines()
    println("Total fuel: ${input.map { getFuel(it.toInt()) }.sum()}")
    println("Total fuel recursive: ${input.map { getTotalFuel(it.toInt()) }.sum()}")
}

fun getFuel(mass: Int) = max(0, floor(mass / 3.0).toInt() - 2)
fun getTotalFuel(mass: Int): Int {
    val fuel = getFuel(mass)
    return if (fuel == 0) 0 else fuel + getTotalFuel(fuel)
}