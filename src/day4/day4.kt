package day4

fun main() {
    val passwordCount = (248345..746315).filter { it.hasDoubleDigits() && it.digitsAreIncreasing() }.size
    println("Password count: $passwordCount")
    val newPasswordCount = (248345..746315).filter { it.hasDoubleDigitsExact() && it.digitsAreIncreasing() }.size
    println("New password count: $newPasswordCount")
}

fun Int.hasDoubleDigits() = this.getDigitGroups().any { it.second >= 2 }
fun Int.hasDoubleDigitsExact() = this.getDigitGroups().any { it.second == 2 }

fun Int.digitsAreIncreasing(): Boolean {
    val digits = this.toString()
    for (i in 0..digits.length - 2) {
        if (digits[i + 1].toInt() < digits[i].toInt()) return false
    }
    return true
}

fun Int.getDigitGroups(): List<Pair<Char, Int>> {
    val digits = this.toString()
    val groups = mutableListOf(digits[0] to 0)
    digits.forEach {
        if (groups.last().first == it)
            groups[groups.size - 1] = it to groups.last().second + 1
        else
            groups.add(it to 1)
    }
    return groups
}