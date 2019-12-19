package day11

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import util.AsyncIntcodeComputer
import util.IntcodeComputer

enum class Color(val value: Char) {
    White('░'),
    Black('▓')
}

@ExperimentalCoroutinesApi
fun main() = runBlocking {
    val input = IntcodeComputer.readProgram("src/day11/input.txt")
    var tiles = mutableMapOf<Pair<Int, Int>, Color>()
    run(input, tiles)
    tiles = mutableMapOf()
    tiles[0 to 0] = Color.White
    run(input, tiles)
}

@ExperimentalCoroutinesApi
fun run(input: List<Long>, tiles: MutableMap<Pair<Int, Int>, Color>) = runBlocking {
    val inputChannel = Channel<Long>()
    val outputChannel = Channel<Long>()
    val computer = AsyncIntcodeComputer(input, inputChannel, outputChannel)
    async { computer.run() }
    var x = 0
    var y = 0
    var dir = 0
    while (!outputChannel.isClosedForReceive) {
        if (tiles[x to y] == Color.White) {
            inputChannel.send(1)
        } else {
            inputChannel.send(0)
        }
        when (outputChannel.receive()) {
            0L -> {
                tiles[x to y] = Color.Black
            }
            1L -> {
                tiles[x to y] = Color.White
            }
            else -> throw Exception("Invalid color")
        }
        when (outputChannel.receive()) {
            0L -> {
                dir--
                if (dir < 0) dir = 3
            }
            1L -> dir = (dir + 1) % 4
            else -> throw Exception("Invalid direction")
        }
        when (dir) {
            0 -> y++
            1 -> x++
            2 -> y--
            3 -> x--
            else -> throw Exception("Invalid facing")
        }
    }
    println("Painted tiles: ${tiles.size}")
    render(tiles)
}

fun render(tiles: Map<Pair<Int, Int>, Color>) {
    val minX = tiles.minBy { it.key.first }?.key?.first ?: 0
    val maxX = tiles.maxBy { it.key.first }?.key?.first ?: 0
    val minY = tiles.minBy { it.key.second }?.key?.second ?: 0
    val maxY = tiles.maxBy { it.key.second }?.key?.second ?: 0
    for (y in maxY downTo minY) {
        println((minX..maxX).joinToString("") { x ->
            tiles[x to y]?.value?.toString() ?: Color.Black.value.toString()
        })
    }
}