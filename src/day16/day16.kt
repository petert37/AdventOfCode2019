import java.io.File

fun main() {

    val basePattern = listOf(0, 1, 0, -1)
    val input = File("src/day16/input.txt").readLines()[0]

    var value = input
    repeat(100) {
        value = getOutput(value, basePattern)
    }
    println(value.substring(0, 8))

    val offset = input.substring(0, 7).toInt(10)
    val newInput = input.repeat(10_000).substring(offset)
    var newValue = newInput
    repeat(100) {
        newValue = getNewOutput(newValue)
    }
    println(newValue.substring(0, 8))

}

fun getMultiplier(basePattern: List<Int>, element: Int, index: Int): Int {
    val realIndex = ((index + 1) / element) % basePattern.size
    return basePattern[realIndex]
}

fun getOutput(input: String, basePattern: List<Int>): String {
    return input.mapIndexed { index, _ ->
        var result = (0).toBigInteger()
        val element = index + 1
        input.forEachIndexed { charIndex, char ->
            result += (char.toString().toInt(10) * getMultiplier(basePattern, element, charIndex)).toBigInteger()
        }
        result.abs() % (10).toBigInteger()
    }.joinToString("")
}

fun getNewOutput(input: String): String {
    var lastDigit = 0
    return input.reversed().map {
        val digit = it.toString().toInt(10)
        val result = (digit + lastDigit) % 10
        lastDigit = result
        result
    }.joinToString("").reversed()
}