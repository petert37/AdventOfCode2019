package day3

import java.io.File
import kotlin.math.abs

typealias Point = Pair<Int, Int>

fun main() {
    val input = File("src/day3/input.txt").readLines()
    val wire1 = Wire(input[0].split(","))
    val wire2 = Wire(input[1].split(","))
    val intersections = wire1.intersect(wire2).toList()
    val closestIntersection = getSmallestPoint(intersections.minus(listOf(0 to 0)))
    println("Closest intersection: $closestIntersection Distance: ${closestIntersection?.let { distance(it) }}")
    val shortestIntersection = shortestIntersection(wire1, wire2, intersections.minus(listOf(0 to 0)))
    println("Shortest intersection: $shortestIntersection Combined steps: ${shortestIntersection?.let {
        wire1.getSteps(
            it
        ) + wire2.getSteps(it)
    }}")
}

class Wire(input: List<String>) {

    private val positions = mutableListOf(0 to 0)

    init {
        input.forEach { move(it) }
    }

    private fun move(move: String) {
        val direction = move.first()
        val distance = move.substring(1).toInt()
        val moveAction: (Point) -> Point = when (direction) {
            'L' -> { point -> point.first - 1 to point.second }
            'R' -> { point -> point.first + 1 to point.second }
            'U' -> { point -> point.first to point.second + 1 }
            'D' -> { point -> point.first to point.second - 1 }
            else -> { point -> point }
        }
        moveLine(distance, moveAction)
    }

    private fun moveLine(distance: Int, moveAction: (Point) -> Point) {
        for (i in 1..distance)
            positions.add(moveAction(positions.last()))
    }

    fun intersect(wire: Wire) = positions.intersect(wire.positions)

    fun getSteps(point: Point) = positions.indexOf(point)
}

fun getSmallestPoint(points: List<Point>) = points.minBy { distance(it) }
fun distance(point: Point) = abs(point.first) + abs(point.second)
fun shortestIntersection(wire1: Wire, wire2: Wire, intersections: List<Point>) =
    intersections.minBy { wire1.getSteps(it) + wire2.getSteps(it) }