package day15

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import util.AsyncIntcodeComputer
import util.IntcodeComputer
import util.render.TileRenderer
import util.render.openTileRendererWindow
import java.awt.Color
import java.awt.Graphics2D

enum class Tile {
    Wall,
    Space,
    OxygenSystem,
    Unknown
}

enum class Direction(val value: Long) {
    North(1),
    South(2),
    West(3),
    East(4);

    fun turnRight(): Direction {
        return when (this) {
            North -> East
            East -> South
            South -> West
            West -> North
        }
    }

    fun turnLeft(): Direction {
        return when (this) {
            North -> West
            East -> North
            South -> East
            West -> South
        }
    }

    fun getMovePosition(position: Pair<Int, Int>): Pair<Int, Int> {
        return when (this) {
            North -> position.first + 1 to position.second
            South -> position.first - 1 to position.second
            East -> position.first to position.second + 1
            West -> position.first to position.second - 1
        }
    }
}

typealias Maze = MutableMap<Pair<Int, Int>, Tile>

fun Maze.surroundWithUnknown(position: Pair<Int, Int>) {
    putIfAbsent(position.first - 1 to position.second, Tile.Unknown)
    putIfAbsent(position.first + 1 to position.second, Tile.Unknown)
    putIfAbsent(position.first to position.second - 1, Tile.Unknown)
    putIfAbsent(position.first to position.second + 1, Tile.Unknown)
}

fun Maze.hasUnknown() = any { it.value == Tile.Unknown }

fun Maze.getNextUnknownPosition(facing: Direction, position: Pair<Int, Int>): Pair<Int, Int>? {
    var dir = facing
    repeat(4) {
        val next = dir.getMovePosition(position)
        if (get(next) == Tile.Unknown) {
            return next
        }
        dir = dir.turnRight()
    }
    return null
}

fun Maze.getNextKnownPosition(facing: Direction, position: Pair<Int, Int>): Pair<Int, Int> {
    val right = facing.turnRight().getMovePosition(position)
    if (get(right) != Tile.Wall) {
        return right
    }
    val straight = facing.getMovePosition(position)
    if (get(straight) != Tile.Wall) {
        return straight
    }
    val left = facing.turnLeft().getMovePosition(position)
    if (get(left) != Tile.Wall) {
        return left
    }
    return facing.turnRight().turnRight().getMovePosition(position)
}

fun Maze.toRenderFormat(): Triple<Int, Int, Map<Pair<Long, Long>, Tile>> {
    val minX = minBy { it.key.first }?.key?.first ?: 0
    val minY = minBy { it.key.second }?.key?.second ?: 0
    val dx = if (minX < 0) -minX else 0
    val dy = if (minY < 0) -minY else 0
    val renderFormat = mapKeys { (it.key.first + dx).toLong() to (it.key.second + dy).toLong() }
    return Triple(dx, dy, renderFormat)
}

fun Maze.getSurroundingPathCoordinates(position: Pair<Int, Int>) = listOf(
    position.first - 1 to position.second,
    position.first + 1 to position.second,
    position.first to position.second - 1,
    position.first to position.second + 1
).filter {
    val tile = get(it)
    return@filter tile == Tile.Space || tile == Tile.OxygenSystem
}

fun main() {
    val input = IntcodeComputer.readProgram("src/day15/input.txt")
    val maze: Maze = mutableMapOf<Pair<Int, Int>, Tile>()
    maze[0 to 0] = Tile.Space
    maze.surroundWithUnknown(0 to 0)
    val canvas = openTileRendererWindow("Day 15", MazeRenderer())
    explore(input, maze, canvas)
    val oxygenSource = maze.filterValues { it == Tile.OxygenSystem }.keys.elementAt(0)
    val distances = calculateDistances(maze, oxygenSource)
    println("Distance to oxygen system: ${distances[0 to 0]}")
    val maxDistance = distances.maxBy { it.value }?.value ?: 0
    println("Max distance form oxygen system: $maxDistance")
}

fun explore(program: List<Long>, maze: Maze, canvas: MazeRenderer) = runBlocking {
    val input = Channel<Long>()
    val output = Channel<Long>()
    val computer = AsyncIntcodeComputer(program, input, output)
    var position = 0 to 0
    var facing = Direction.North

    val computerExecution = launch { computer.run() }

    while (maze.hasUnknown()) {
        input.send(facing.value)
        val movePosition = facing.getMovePosition(position)
        when (val result = output.receive()) {
            0L -> {
                maze[movePosition] = Tile.Wall
            }
            1L -> {
                maze[movePosition] = Tile.Space
                position = movePosition
            }
            2L -> {
                maze[movePosition] = Tile.OxygenSystem
                position = movePosition
            }
            else -> throw Exception("Unknown output: $result")
        }
        maze.surroundWithUnknown(position)
        val nextPosition = maze.getNextUnknownPosition(facing, position) ?: maze.getNextKnownPosition(facing, position)
        while (facing.getMovePosition(position) != nextPosition) {
            facing = facing.turnRight()
        }
        val renderFormat = maze.toRenderFormat()
        canvas.startPosition = 0 + renderFormat.first to 0 + renderFormat.second
        canvas.robotPosition = position.first + renderFormat.first to position.second + renderFormat.second
        canvas.render(renderFormat.third)
//        delay(10)
    }

    computerExecution.cancel()
}

fun calculateDistances(maze: Maze, start: Pair<Int, Int>): Map<Pair<Int, Int>, Int> {
    val distances = mutableMapOf<Pair<Int, Int>, Int>()
    distances[start] = 0
    calculateSurroundingDistances(maze, start, distances)
    return distances
}

fun calculateSurroundingDistances(maze: Maze, position: Pair<Int, Int>, distances: MutableMap<Pair<Int, Int>, Int>) {
    val distance = distances[position] ?: 0
    getUnvisitedSurroundingPositions(maze, position, distances).forEach {
        distances[it] = distance + 1
        calculateSurroundingDistances(maze, it, distances)
    }
}

fun getUnvisitedSurroundingPositions(maze: Maze, position: Pair<Int, Int>, distances: Map<Pair<Int, Int>, Int>) =
    maze.getSurroundingPathCoordinates(position).filter { distances[it] == null }

class MazeRenderer : TileRenderer<Tile>(Color.GRAY) {

    var robotPosition = 0 to 0
    var startPosition = 0 to 0

    override fun renderTile(graphics: Graphics2D, tile: Tile?, x: Long, y: Long, tileSize: Double) {
        val color = when (tile ?: Tile.Unknown) {
            Tile.Unknown -> Color.GRAY
            Tile.Space -> Color.WHITE
            Tile.OxygenSystem -> Color.BLUE
            Tile.Wall -> Color.BLACK
        }
        drawRect(graphics, color, x, y)
    }

    override fun renderGame(g: Graphics2D) {
        super.renderGame(g)
        drawRect(g, Color.GREEN, startPosition.first.toLong(), startPosition.second.toLong())
        drawCircle(g, Color.RED, robotPosition.first.toLong(), robotPosition.second.toLong())
    }
}