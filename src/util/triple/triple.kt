package util.triple

val <T, U, V> Triple<T, U, V>.x: T
    get() = first

val <T, U, V> Triple<T, U, V>.y: U
    get() = second

val <T, U, V> Triple<T, U, V>.z: V
    get() = third