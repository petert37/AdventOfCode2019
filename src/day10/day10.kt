package day10

import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

typealias Position = Pair<Int, Int>

fun main() {
    val asteroids = mutableSetOf<Position>()
    File("src/day10/input.txt").readLines().forEachIndexed { y, line ->
        line.forEachIndexed { x, char ->
            if (char == '#') {
                asteroids.add(x to y)
            }
        }
    }
    val visibleAsteroids = asteroids.associateWith { getVisibleAsteroids(it, asteroids.minus(it)) }
    val bestMonitoringStation = visibleAsteroids.maxBy { it.value.size }
    println("Best monitoring station ${bestMonitoringStation?.key}, visible asteroids: ${bestMonitoringStation?.value?.size}")
    val destroyed = mutableListOf<Position>()
    bestMonitoringStation?.let {
        destroyAsteroids(
            bestMonitoringStation.key,
            asteroids.minus(bestMonitoringStation.key).toMutableSet(),
            destroyed
        )
    }
    println("No 200 destroyed asteroid ${destroyed.getOrNull(199)}, answer: ${destroyed.getOrNull(199)?.let { it.first * 100 + it.second }}")
}

fun getVisibleAsteroids(asteroid: Position, asteroids: Collection<Position>): List<Position> {
    return asteroids.filter { isVisible(asteroid, it, asteroids.minus(it)) }
}

fun isVisible(p1: Position, p2: Position, asteroids: Collection<Position>): Boolean {
    return asteroids.filter { isOnLine(it, p1, p2) }.none { isInBoundingBox(it, p1, p2) }
}

fun getLineEquation(p1: Position, p2: Position): (Int) -> Double {
    val a = (p2.second - p1.second).toDouble() / (p2.first - p1.first).toDouble()
    val b = p1.second.toDouble() - a * p1.first.toDouble()
    return { x -> a * x + b }
}

fun isOnLine(p: Position, p1: Position, p2: Position): Boolean {
    if (p.first == p2.first)
        return p.first == p1.first
    val lineEquation = getLineEquation(p1, p2)
    val y = lineEquation(p.first)
    return abs(y - p.second.toDouble()) < 0.00001
}

fun getAngle(p1: Position, p2: Position): Double {
    val angle = atan2((-p2.second + p1.second).toDouble(), (p2.first - p1.first).toDouble())
    if (angle >= 0 && angle <= PI / 2.0)
        return PI / 2.0 - angle
    if (angle > PI / 2.0)
        return PI + PI / 2.0 + (PI - angle)
    if (angle < 0)
        return PI / 2.0 - angle
    return 0.0
}

fun isInBoundingBox(p: Position, b1: Position, b2: Position): Boolean {
    val minX = if (b1.first < b2.first) b1.first else b2.first
    val maxX = if (b1.first >= b2.first) b1.first else b2.first
    val minY = if (b1.second < b2.second) b1.second else b2.second
    val maxY = if (b1.second >= b2.second) b1.second else b2.second
    return p.first in minX..maxX && p.second in minY..maxY
}

fun destroyAsteroids(center: Position, asteroids: MutableCollection<Position>, destroyed: MutableList<Position>) {
    if (asteroids.isEmpty())
        return
    val lastAsteroid = destroyed.lastOrNull()
    var target: Position?
    val visible = getVisibleAsteroids(center, asteroids)
    if (lastAsteroid != null) {
        val lastAngle = getAngle(center, lastAsteroid)
        target = visible.filter { getAngle(center, it) > lastAngle }.minBy { getAngle(center, it) }
        if (target == null) {
            target = visible.minBy { getAngle(center, it) }
        }
    } else {
        target = visible.minBy { getAngle(center, it) }
    }
    if (target != null) {
        asteroids.remove(target)
        destroyed.add(target)
        destroyAsteroids(center, asteroids, destroyed)
    } else {
        throw Exception("Something vent wrong")
    }
}