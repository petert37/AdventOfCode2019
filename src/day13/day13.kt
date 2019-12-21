package day13

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import util.AsyncIntcodeComputer
import util.IntcodeComputer
import java.awt.*
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign


fun main() = runBlocking {
    val input = IntcodeComputer.readProgram("src/day13/input.txt")
    var output = Channel<Long>()
    var computer = AsyncIntcodeComputer(input, Channel(), output)
    var x = 0L
    var y = 0L
    var tile: Tile
    var outputIndex = 0
    var tiles = mutableMapOf<Pair<Long, Long>, Tile>()

    launch { computer.run() }

    for (value in output) {
        when (outputIndex) {
            0 -> x = value
            1 -> y = value
            2 -> {
                tile = Tile.fromCode(value.toInt()) ?: throw Exception("Tile type not found")
                tiles[x to y] = tile
            }
        }
        outputIndex = (outputIndex + 1) % 3
    }

    println("Block tiles: ${tiles.count { it.value == Tile.Block }}")

    val inputChannel = Channel<Long>(1)
    output = Channel()
    val gameInput = input.toMutableList()
    gameInput[0] = 2L
    tiles = mutableMapOf()
    var score = 0L
    var previousBallPosition: Pair<Long, Long>? = null
    var ballPosition: Pair<Long, Long>? = null
    var paddlePosition: Pair<Long, Long>? = null

    val canvas = showGame()

    computer = AsyncIntcodeComputer(gameInput, inputChannel, output, beforeInput = {
        delay(10)
        val key = calculateInput(tiles, previousBallPosition, ballPosition, paddlePosition)
        canvas.render(tiles.toMap(), score)
        inputChannel.send(key)
    })

    launch { computer.run() }

    for (value in output) {
        when (outputIndex) {
            0 -> x = value
            1 -> y = value
            2 -> {
                if (x == -1L && y == 0L) {
                    score = value
                } else {
                    tile = Tile.fromCode(value.toInt()) ?: throw Exception("Tile type not found")
                    tiles[x to y] = tile
                    if (tile == Tile.Ball) {
                        previousBallPosition = ballPosition
                        ballPosition = x to y
                    }
                    if (tile == Tile.HorizontalPaddle) {
                        paddlePosition = x to y
                    }
                }
            }
        }
        outputIndex = (outputIndex + 1) % 3
    }

    canvas.render(tiles.toMap(), score)
    println("Final score: $score")

}

fun calculateInput(
    tiles: Map<Pair<Long, Long>, Tile>,
    previousBallPosition: Pair<Long, Long>?,
    ballPosition: Pair<Long, Long>?,
    paddlePosition: Pair<Long, Long>?
): Long {

    if (previousBallPosition == null || ballPosition == null || paddlePosition == null)
        return 0L

    val targetX = simulateBallPath(tiles, ballPosition, previousBallPosition, paddlePosition.second - 1)
    return (targetX - paddlePosition.first).sign.toLong()
}

fun simulateBallPath(
    tiles: Map<Pair<Long, Long>, Tile>,
    ballPosition: Pair<Long, Long>,
    previousBallPosition: Pair<Long, Long>,
    targetY: Long
): Long {

    val t = tiles.toMutableMap()
    var ball = ballPosition
    var dx = (ballPosition.first - previousBallPosition.first).sign.toLong()
    var dy = (ballPosition.second - previousBallPosition.second).sign.toLong()


    while (ball.second != targetY) {
        var bounced = false
        val x = ball.first
        val y = ball.second
        if (t[x + dx to y] == Tile.Wall) {
            dx *= -1
            bounced = true
        }
        if (t[x + dx to y] == Tile.Block) {
            t[x + dx to y] = Tile.Empty
            dx *= -1
            bounced = true
        }
        if (t[x to y + dy] == Tile.Wall) {
            dy *= -1
            bounced = true
        }
        if (t[x to y + dy] == Tile.Block) {
            t[x to y + dy] = Tile.Empty
            dy *= -1
            bounced = true
        }
        if (!bounced) {
            if (t[x + dx to y + dy] == Tile.Wall) {
                dx *= -1
                dy *= -1
                bounced = true
            }
            if (t[x + dx to y + dy] == Tile.Block) {
                t[x + dx to y + dy] = Tile.Empty
                dx *= -1
                dy *= -1
                bounced = true
            }
        }
        if (!bounced) {
            t[x to y] = Tile.Empty
            t[x + dx to y + dy] = Tile.Ball
            ball = x + dx to y + dy
        }
    }

    return ball.first

}

enum class Tile(val code: Int) {
    Empty(0),
    Wall(1),
    Block(2),
    HorizontalPaddle(3),
    Ball(4);

    companion object {
        private val map = values().associateBy(Tile::code)
        fun fromCode(code: Int) = map[code]
    }
}

fun showGame(): Canvas {
    val frame = JFrame("Advent of Code 2019 Day 13")
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    val canvas = Canvas()
    frame.add(canvas)
    frame.size = Dimension(800, 600)
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
    return canvas
}

class Canvas : JPanel() {

    private var score = 0L
    private var tiles: Map<Pair<Long, Long>, Tile> = emptyMap()

    override fun paint(g: Graphics?) {
        super.paint(g)
        if (graphics == null)
            return
        g as Graphics2D
        renderGame(g)
    }

    fun render(tiles: Map<Pair<Long, Long>, Tile>, score: Long) {
        this.tiles = tiles
        this.score = score
        repaint()
    }

    private fun renderGame(g: Graphics2D) {
        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)

        val minX = tiles.keys.minBy { it.first }?.first ?: 0
        val maxX = tiles.keys.maxBy { it.first }?.first ?: 0
        val minY = tiles.keys.minBy { it.second }?.second ?: 0
        val maxY = tiles.keys.maxBy { it.second }?.second ?: 0
        val xTiles = maxX - minX + 1
        val yTiles = maxY - minY + 1
        val xSize = width / if (xTiles == 0L) 1.0 else xTiles.toDouble()
        val ySize = (height - 30) / if (yTiles == 0L) 1.0 else yTiles.toDouble()
        val tileSize = min(xSize, ySize)


        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val tile = tiles[x to y] ?: Tile.Empty
                when (tile) {
                    Tile.Empty -> {
                    }
                    Tile.Wall -> {
                        g.color = Color.LIGHT_GRAY
                        g.fillRect(
                            (tileSize * x).roundToInt(),
                            (tileSize * y).roundToInt(),
                            tileSize.roundToInt(),
                            tileSize.roundToInt()
                        )
                    }
                    Tile.Block -> {
                        g.color = Color.BLUE
                        g.fillRect(
                            (tileSize * x).roundToInt(),
                            (tileSize * y).roundToInt(),
                            tileSize.roundToInt(),
                            tileSize.roundToInt()
                        )
                    }
                    Tile.HorizontalPaddle -> {
                        g.color = Color.RED
                        g.fillRect(
                            (tileSize * x).roundToInt(),
                            (tileSize * y).roundToInt(),
                            tileSize.roundToInt(),
                            (tileSize * 0.5).roundToInt()
                        )
                    }
                    Tile.Ball -> {
                        g.color = Color.WHITE
                        g.fillOval(
                            (tileSize * x).roundToInt(),
                            (tileSize * y).roundToInt(),
                            tileSize.roundToInt(),
                            tileSize.roundToInt()
                        )
                    }
                }
            }
        }

        g.font = Font("TimesRoman", Font.PLAIN, 20)
        g.drawString("Score: $score", 0, height - 2)

    }

}