package util.permutations

import java.util.*

interface Circular<T> : Iterable<T> {
    fun state(): T
    fun inc()
    fun isZero(): Boolean   // `true` in exactly one state
    fun hasNext(): Boolean  // `false` if the next state `isZero()`

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            var started = false;

            override fun hasNext() = this@Circular.hasNext()

            override fun next(): T {
                if (started)
                    inc()
                else
                    started = true
                return state()
            }

        }
    }
}

class Ring(val size: Int) : Circular<Int> {

    private var state = 0

    override fun state() = state

    override fun inc() {
        state = (state + 1) % size
    }

    override fun isZero() = state == 0

    override fun hasNext() = state != size - 1

}

abstract class CircularList<T, U : Circular<T>>(val size: Int) : Circular<List<T>> {

    protected abstract val state: List<U>

    override fun inc() {
        state.forEach {
            it.inc()
            if (!it.isZero()) return@inc
        }
    }

    override fun isZero() = state.all { it.isZero() }

    override fun hasNext() = state.any { it.hasNext() }

}

class Permutations(n: Int, private val start: Int = 0) : CircularList<Int, Ring>(n) {

    override val state = mutableListOf<Ring>()

    init {
        for (i in n downTo 1) {
            state += Ring(i)
        }
    }

    override fun state(): List<Int> {
        val items = (start until start + size).toCollection(LinkedList())
        return state.map { ring -> items.removeAt(ring.state()) }
    }
}