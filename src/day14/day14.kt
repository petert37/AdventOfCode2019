package day14

import java.io.File
import kotlin.math.ceil

fun main() {
    val input = File("src/day14/input.txt")
        .readLines()
        .map { it.split(" => ") }
        .associate {
            val inputs = it[0].split(", ").map { input ->
                val inputValue = input.split(" ")
                Chemical(inputValue[1], inputValue[0].toLong())
            }
            val outputValue = it[1].split(" ")
            val outputAmount = outputValue[0].toLong()
            val outputName = outputValue[1]
            outputName to Reaction(inputs, Chemical(outputName, outputAmount))
        }

    var total = mutableMapOf<String, Long>()
    make(Chemical("FUEL", 1), input, mutableMapOf(), total)
    val totalForOneFuel = total["ORE"] ?: 0
    println("ORE to make 1 FUEL: $totalForOneFuel")

    val ore = 1_000_000_000_000L
    //Multiplier is random guess, it works without it, but slower
    //Could use some kind of binary search like method to get closer to the answer
    var fuelToMake = (ore / totalForOneFuel * 1.275).toLong()
    while (true) {
        val storage = mutableMapOf<String, Long>()
        total = mutableMapOf()
        make(Chemical("FUEL", fuelToMake), input, storage, total)
        val oreUsed = total["ORE"] ?: 0
        System.out.printf("\rOre used: %d", oreUsed)
        if (oreUsed > ore) {
            break
        }
        fuelToMake++
    }
    println()
    fuelToMake--
    println("Total fuel from provided ore: $fuelToMake")
}

fun make(
    chemical: Chemical,
    reactions: Map<String, Reaction>,
    storage: MutableMap<String, Long>,
    total: MutableMap<String, Long>,
    rawResource: String = "ORE"
) {
    val required = chemical.amount
    val stored = storage[chemical.name] ?: 0
    if (stored >= required) {
        storage[chemical.name] = stored - required
        return
    }
    val toMake = required - stored
    if (chemical.name == rawResource) {
        total.merge(rawResource, toMake) { currentValue, newValue -> currentValue + newValue }
        return
    }
    val reaction = reactions[chemical.name] ?: throw Exception("Reaction not found")
    val multiplier = ceil(toMake.toDouble() / reaction.output.amount.toDouble()).toInt()
    reaction.input.forEach { make(Chemical(it.name, multiplier * it.amount), reactions, storage, total) }
    val made = multiplier * reaction.output.amount
    total.merge(chemical.name, made) { currentValue, newValue -> currentValue + newValue }
    storage[chemical.name] = made - toMake
}

data class Reaction(val input: List<Chemical>, val output: Chemical)

data class Chemical(val name: String, val amount: Long)