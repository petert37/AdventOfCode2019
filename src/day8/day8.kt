package day8

import java.io.File

private typealias Layer = List<List<Int>>

fun main() {
    val input = File("src/day8/input.txt").readText()
    val rows = 6
    val columns = 25
    val image = SpaceImage(input, rows, columns)

    image.layers.minBy { layer -> layer.flatten().count { it == 0 } }?.flatten()?.let { flatLayer ->
        println("Not corrupted: ${flatLayer.count { it == 1 } * flatLayer.count { it == 2 }}")
    }

    image.composed.forEach {
        println(it.joinToString("") { char ->
            when (char) {
                0 -> "▓"
                1 -> "░"
                else -> " "
            }
        })
    }
}

class SpaceImage(private val input: String, private val rows: Int, private val columns: Int) {

    private val layerCount = input.length / (rows * columns)
    val layers: List<Layer>

    init {
        layers = List(layerCount) { layer ->
            List(rows) { row ->
                List(columns) { column ->
                    input[layer * (rows * columns) + row * columns + column].toString().toInt()
                }
            }
        }
    }

    val composed: Layer by lazy {
        List(rows) { row ->
            List(columns) { column ->
                getPixel(row, column)
            }
        }
    }

    private fun getPixel(row: Int, column: Int): Int {
        layers.forEach {
            val value = it[row][column]
            if (value != 2) {
                return value
            }
        }
        return -1
    }

}