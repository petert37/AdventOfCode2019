package day7

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import util.AsyncIntcodeComputer
import util.IntcodeComputer
import util.permutations.Permutations

fun main() = runBlocking {
    val input = IntcodeComputer.readProgram("src/day7/input.txt")
    var max = Long.MIN_VALUE

    Permutations(5).forEach { permutation ->

        val inChannel = Channel<Long>(1)

        var outChannel = inChannel

        runBlocking {
            permutation.forEach { phase ->
                val newOutChannel = Channel<Long>(1)
                val computer = AsyncIntcodeComputer(input, outChannel, newOutChannel)
                outChannel.send(phase.toLong())
                outChannel = newOutChannel
                async { computer.run() }
            }
            inChannel.send(0)

            val output = outChannel.receive()
            if (output > max) max = output

        }

    }
    println("Largest output: $max")

    max = Long.MIN_VALUE

    Permutations(5, 5).forEach { permutation ->

        val inChannel = Channel<Long>(1)

        var outChannel = inChannel

        runBlocking {

            permutation.forEach { phase ->
                val newOutChannel = Channel<Long>(1)
                val computer = AsyncIntcodeComputer(input, outChannel, newOutChannel)
                outChannel.send(phase.toLong())
                outChannel = newOutChannel
                async { computer.run() }
            }

            inChannel.send(0)

            for (value in outChannel) {
                if (value > max) {
                    max = value
                }
                inChannel.send(value)
            }

        }

    }
    println("Largest feedback output: $max")
}