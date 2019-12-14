package day6

import util.Node
import java.io.File

typealias Orbit = Pair<String, String>

fun main() {
    val input = File("src/day6/input.txt")
        .readLines()
        .map { it.split(")") }
        .map { it[0] to it[1] }

    val root = Node("COM")
    addChildren(root, input)
    val allDirectOrbits = root.getAllChildren()
    val totalOrbitCount = allDirectOrbits.map { it.getUpwardDistanceTo(root) }.sum()
    println("Direct orbits: ${allDirectOrbits.size} Total: $totalOrbitCount")
    val you = allDirectOrbits.find { it.value == "YOU" } ?: throw Exception("YOU not found")
    val santa = allDirectOrbits.find { it.value == "SAN" } ?: throw Exception("SAN not found")
    val transferDistance = getTransferDistance(you, santa)
    println("Transfer distance: $transferDistance")
}

fun addChildren(node: Node<String>, orbits: List<Orbit>) {
    orbits.filter { it.first == node.value }.forEach { node.addChild(Node(it.second)) }
    node.children.forEach { addChildren(it, orbits) }
}

fun getTransferDistance(from: Node<String>, to: Node<String>): Int {
    var distance = 0
    var parent = from.parent ?: throw Exception("Cannot move COM")
    while (true) {
        val children = parent.getAllChildren()
        if (children.contains(to)) {
            distance += to.getUpwardDistanceTo(parent) - 1
            break
        }
        if (parent.parent == null) {
            throw Exception("Transfer not possible")
        }
        parent = parent.parent!!
        distance++
    }
    return distance
}