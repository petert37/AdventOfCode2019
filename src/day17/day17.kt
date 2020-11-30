import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import util.AsyncIntcodeComputer
import util.IntcodeComputer

@ExperimentalCoroutinesApi
fun main() {
    val program = IntcodeComputer.readProgram("src/day17/input.txt")
    val cameraImageRaw = runBlocking {
        val input = Channel<Long>()
        val output = Channel<Long>()
        val computer = AsyncIntcodeComputer(program, input, output)
        launch { computer.run() }
        val sb = StringBuilder()
        for (char in output) {
            sb.append(char.toChar())
        }
        sb.toString()
    }
    println(cameraImageRaw)
    val cameraImage = CameraImage(cameraImageRaw)
    var result = 0
    for (x in 0 until cameraImage.width) {
        for (y in 0 until cameraImage.height) {
            val char = cameraImage.get(x, y)
            if (char == '#') {
                val surrounding = cameraImage.getSurrounding(x, y)
                if (surrounding.size == 4 && surrounding.all { it == '#' }) {
                    result += x * y
                }
            }
        }
    }
    println(result)

    val path = mutableListOf<String>()
    while (true) {
        val straight = cameraImage.getStraightPathLength()
        if (straight > 0) {
            path.add(straight.toString())
            cameraImage.move(straight)
            continue
        }
        val turn = cameraImage.getDirectionToTurn()
        if (turn != null) {
            val turnString = if (turn == cameraImage.robotDirection.left()) {
                "L"
            } else {
                "R"
            }
            path.add(turnString)
            cameraImage.turn(turn)
            continue
        }
        break
    }

    val steps: List<Step> = path.map { Instruction(it) }
    val movementFunctions = stepsToMovementFunctions2(steps)

    val newProgram = program.toMutableList()
    newProgram[0] = 2

    runBlocking {
        val input = Channel<Long>()
        val output = Channel<Long>()
        val computer = AsyncIntcodeComputer(newProgram, input, output, false)
        launch { computer.run() }
        val dust = async {
            for (char in output) {
                if (!output.isClosedForReceive) {
                    print(char.toChar())
                } else {
                    return@async char
                }
            }
            0L
        }
        movementFunctions.forEachIndexed { index, movementFunction ->
            if (index != 0) {
                writeCharToInput(input, ',')
            }
            writeCharToInput(input, movementFunction.name[0])
        }
        writeCharToInput(input, '\n')
        movementFunctions.distinct().sortedBy { it.name[0].toLong() }.forEach {
            it.instructions.forEachIndexed { index, instruction ->
                if (index != 0) {
                    writeCharToInput(input, ',')
                }
                instruction.value.forEach { char ->
                    writeCharToInput(input, char)
                }
            }
            writeCharToInput(input, '\n')
        }
        writeCharToInput(input, 'n')
        writeCharToInput(input, '\n')
        val finalOutput = dust.await()
        println()
        println("Dust: $finalOutput")
    }
}

suspend fun writeCharToInput(input: SendChannel<Long>, char: Char) {
    input.send(char.toLong())
    print(char)
}

fun calculateCharacterCount(steps: List<Step>): Int {
    val instructions = steps.filterIsInstance<Instruction>()
    return instructions.sumBy { it.value.length } + instructions.size - 1
}

fun stepsToMovementFunctions2(steps: List<Step>): List<MovementFunction> {
    for (a in 1..10) {
        for (b in 1..10) {
            for (c in 1..10) {
                if (a > steps.size || calculateCharacterCount(steps.subList(0, a)) > 20) {
                    continue
                }
                val withoutA = removeOccurrences(steps, 0..a)
                if (b > withoutA.size || calculateCharacterCount(withoutA.subList(0, b)) > 20) {
                    continue
                }
                val withoutB = removeOccurrences(withoutA, 0..b)
                if (c > withoutB.size || calculateCharacterCount(withoutB.subList(0, c)) > 20) {
                    continue
                }
                val withoutC = removeOccurrences(withoutB, 0..c)
                if (withoutC.isNotEmpty()) {
                    continue
                }
                val stepsA = replaceOccurrences(
                    steps,
                    0..a,
                    MovementFunction("A", steps.subList(0, a).map { it as Instruction })
                )
                val firstAStepIndex = stepsA.indexOfFirst { it is Instruction }
                val stepsB = replaceOccurrences(
                    stepsA,
                    firstAStepIndex..(firstAStepIndex + b),
                    MovementFunction("B", withoutA.subList(0, b).map { it as Instruction })
                )
                val firstBStepIndex = stepsB.indexOfFirst { it is Instruction }
                val stepsC = replaceOccurrences(
                    stepsB,
                    firstBStepIndex..(firstBStepIndex + b),
                    MovementFunction("C", withoutB.subList(0, c).map { it as Instruction })
                )
                return stepsC.map { it as MovementFunction }
            }
        }
    }
    return emptyList()
}

fun replaceOccurrences(steps: List<Step>, toReplace: IntRange, replacement: MovementFunction): List<Step> {
    val occurrences = findOccurrences(steps, toReplace)
    val currentSteps = removeOccurrences(steps, toReplace).toMutableList()
    occurrences.forEachIndexed { index, intRange ->
        currentSteps.add(intRange.first - replacement.instructions.size * index + index, replacement)
    }
    return currentSteps
}

fun removeOccurrences(steps: List<Step>, toRemove: IntRange): List<Step> {
    val rangesToRemove = findOccurrences(steps, toRemove)
    return steps.filterIndexed { index, _ -> !rangesToRemove.any { it.contains(index) } }
}

fun findOccurrences(steps: List<Step>, toFind: IntRange): List<IntRange> {
    val subList = steps.subList(toFind.first, toFind.last)
    val startIndices = mutableListOf<Int>()
    for (i in 0..(steps.size - subList.size)) {
        if (subList == steps.subList(i, i + subList.size)) {
            startIndices.add(i)
        }
    }
    return startIndices.map { it until it + subList.size }
}

class CameraImage(rawCameraImage: String) {

    private val cameraImage: List<CharArray>
    val height: Int
    val width: Int
    private var robotX: Int = 0
    private var robotY: Int = 0
    var robotDirection = Direction.NORTH

    init {
        val tmpImage = rawCameraImage.split("\n").filter { it.isNotEmpty() }.map { it.toCharArray() }
        height = tmpImage.size
        width = tmpImage[0].size
        val directionChars = Direction.values().map(Direction::char)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val char = tmpImage[y][x]
                if (char in directionChars) {
                    robotX = x
                    robotY = y
                    tmpImage[y][x] = '#'
                    robotDirection = Direction.fromChar(char)
                }
            }
        }
        cameraImage = tmpImage
    }

    fun get(x: Int, y: Int): Char? {
        return if (x in 0 until width && y in 0 until height) {
            cameraImage[y][x]
        } else {
            null
        }
    }

    fun getSurrounding(x: Int, y: Int) =
        listOfNotNull(get(x, y - 1), get(x, y + 1), get(x - 1, y), get(x + 1, y))

    fun getStraightPathLength(): Int {
        var length = 0
        var x = robotX
        var y = robotY
        while (nextInDirection(x, y, robotDirection) == '#') {
            length++
            x += robotDirection.xOffset
            y += robotDirection.yOffset
        }
        return length
    }

    private fun nextInDirection(x: Int, y: Int, direction: Direction) =
        get(x + direction.xOffset, y + direction.yOffset)

    fun getDirectionToTurn(): Direction? {
        val left = robotDirection.left()
        if (get(robotX + left.xOffset, robotY + left.yOffset) == '#') {
            return left
        }
        val right = robotDirection.right()
        if (get(robotX + right.xOffset, robotY + right.yOffset) == '#') {
            return right
        }
        return null
    }

    fun turn(direction: Direction) {
        robotDirection = direction
    }

    fun move(steps: Int) {
        robotX += steps * robotDirection.xOffset
        robotY += steps * robotDirection.yOffset
    }

}

enum class Direction(val char: Char, val xOffset: Int, val yOffset: Int) {
    NORTH('^', 0, -1),
    EAST('>', 1, 0),
    SOUTH('v', 0, 1),
    WEST('<', -1, 0);

    fun left(): Direction {
        return when (this) {
            NORTH -> WEST
            EAST -> NORTH
            SOUTH -> EAST
            WEST -> SOUTH
        }
    }

    fun right(): Direction {
        return when (this) {
            NORTH -> EAST
            EAST -> SOUTH
            SOUTH -> WEST
            WEST -> NORTH
        }
    }

    companion object {
        private val map = values().associateBy(Direction::char)

        fun fromChar(char: Char) = map[char] ?: error("Unknown direction")
    }
}

interface Step

data class Instruction(val value: String) : Step

data class MovementFunction(val name: String, val instructions: List<Instruction>) : Step