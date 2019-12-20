package day12

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import util.triple.x
import util.triple.y
import util.triple.z
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

fun main() {
    val moons = File("src/day12/input.txt")
        .readLines()
        .map { it.removeSurrounding("<", ">").split(", ") }
        .map { line -> line.map { it.split("=")[1] } }
        .map { Moon(it[0].toInt(), it[1].toInt(), it[2].toInt()) }
    val firstMoons = moons.map { Moon(it) }
    repeat(1000) {
        simulateTimeStep(firstMoons)
    }
    println("Total energy: ${firstMoons.sumBy { it.energy }}")
    val secondMoons = moons.map { Moon(it) }
    val steps = getCycle(secondMoons)
    println("Steps to return to previous state: $steps")
}

fun simulateTimeStep(moons: List<Moon>) {
    simulateGravity(moons)
    moons.forEach { it.applyVelocity() }
}

fun simulateTimeStepDimension(dimensions: List<MoonDimension>) {
    simulateGravityDimension(dimensions)
    dimensions.forEach { it.applyVelocity() }
}

tailrec fun simulateGravity(moons: List<Moon>) {
    if (moons.size < 2)
        return
    val moon = moons.first()
    for (i in 1 until moons.size) {
        moon.applyGravity(moons[i])
        moons[i].applyGravity(moon)
    }
    simulateGravity(moons.drop(1))
}

tailrec fun simulateGravityDimension(dimensions: List<MoonDimension>) {
    if (dimensions.size < 2)
        return
    val dimension = dimensions.first()
    for (i in 1 until dimensions.size) {
        dimension.applyGravity(dimensions[i])
        dimensions[i].applyGravity(dimension)
    }
    simulateGravityDimension(dimensions.drop(1))
}

fun getDimensionCycle(dimensions: List<MoonDimension>): Int {
    val initialState = dimensions.toString()
    var steps = 0
    while (true) {
        steps++
        simulateTimeStepDimension(dimensions)
        val state = dimensions.toString()
        if (state == initialState) {
            break
        }
    }
    return steps
}

fun getCycle(moons: List<Moon>) = runBlocking {
    val xJob = async { getDimensionCycle(moons.map { MoonDimension(it.position.x) }) }
    val yJob = async { getDimensionCycle(moons.map { MoonDimension(it.position.y) }) }
    val zJob = async { getDimensionCycle(moons.map { MoonDimension(it.position.z) }) }
    return@runBlocking lcm(lcm(xJob.await().toLong(), yJob.await().toLong()), zJob.await().toLong())
}

fun lcm(number1: Long, number2: Long): Long {
    if (number1 == 0L || number2 == 0L) {
        return 0
    }
    val absNumber1 = abs(number1)
    val absNumber2 = abs(number2)
    val absHigherNumber = max(absNumber1, absNumber2)
    val absLowerNumber = min(absNumber1, absNumber2)
    var lcm = absHigherNumber
    while (lcm % absLowerNumber != 0L) {
        lcm += absHigherNumber
    }
    return lcm
}

class Moon(x: Int, y: Int, z: Int) {

    var position: Triple<Int, Int, Int> = Triple(x, y, z)
    var velocity: Triple<Int, Int, Int> = Triple(0, 0, 0)

    constructor(moon: Moon) : this(moon.position.x, moon.position.y, moon.position.z) {
        velocity = Triple(moon.velocity.x, moon.velocity.y, moon.velocity.z)
    }

    fun applyGravity(moon: Moon) {
        velocity = Triple(
            velocity.x + (moon.position.x - position.x).sign,
            velocity.y + (moon.position.y - position.y).sign,
            velocity.z + (moon.position.z - position.z).sign
        )
    }

    fun applyVelocity() {
        position = Triple(
            position.x + velocity.x,
            position.y + velocity.y,
            position.z + velocity.z
        )
    }

    private val potentialEnergy
        get() = abs(position.x) + abs(position.y) + abs(position.z)

    private val kineticEnergy
        get() = abs(velocity.x) + abs(velocity.y) + abs(velocity.z)

    val energy
        get() = potentialEnergy * kineticEnergy

    override fun toString() =
        "pos=<x=${position.x}, y=${position.y}, z=${position.z}>, vel=<x=${velocity.x}, y=${velocity.y}, z=${velocity.z}>"

}

class MoonDimension(positionDimension: Int) {
    var position = positionDimension
    var velocity = 0

    fun applyGravity(dimension: MoonDimension) {
        velocity += (dimension.position - position).sign
    }

    fun applyVelocity() {
        position += velocity
    }

    override fun toString() = "$position#$velocity"
}