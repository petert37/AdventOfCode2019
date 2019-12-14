package util

class Node<T>(var value: T) {

    var children = mutableListOf<Node<T>>()
    var parent: Node<T>? = null

    fun addChild(node: Node<T>) {
        children.add(node)
        node.parent = this
    }

    fun getAllChildren(): List<Node<T>> {
        return if (children.isEmpty()) emptyList() else children.plus(children.flatMap { it.getAllChildren() })
    }

    fun getUpwardDistanceTo(node: Node<T>): Int {
        var distance = 0;
        var parent = this.parent
        while (true) {
            distance++
            if (parent == null || parent == node)
                break
            parent = parent.parent
        }
        return distance
    }

    override fun toString(): String {
        var s = "${value}"
        if (!children.isEmpty()) {
            s += " {${children.map { it.toString() }}}"
        }
        return s
    }
}