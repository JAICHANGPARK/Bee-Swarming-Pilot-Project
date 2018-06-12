package com.dreamwalker.knu2018.bluno

class RingBuffer<T>// cast needed since no generic array creation in Java
(capacity: Int) {

    private val buffer: Array<T>          // queue elements
    private var count = 0          // number of elements on queue
    private var indexOut = 0       // index of first element of queue
    private var indexIn = 0       // index of next available slot

    val isEmpty: Boolean
        get() = count == 0

    val isFull: Boolean
        get() = count == buffer.size

    init {
        buffer = arrayOfNulls<Any>(capacity) as Array<T>
    }

    fun size(): Int {
        return count
    }

    fun clear() {
        count = 0
    }

    fun push(item: T) {
        if (count == buffer.size) {
            println("Ring buffer overflow")
            //            throw new RuntimeException("Ring buffer overflow");
        }
        buffer[indexIn] = item
        indexIn = (indexIn + 1) % buffer.size     // wrap-around
        if (count++ == buffer.size) {
            count = buffer.size
        }
    }

    fun pop(): T {
        if (isEmpty) {
            println("Ring buffer pop underflow")

            //            throw new RuntimeException("Ring buffer underflow");
        }
        val item = buffer[indexOut]
        buffer[indexOut] = null                  // to help with garbage collection
        if (count-- == 0) {
            count = 0
        }
        indexOut = (indexOut + 1) % buffer.size // wrap-around
        return item
    }

    operator fun next(): T {
        if (isEmpty) {
            println("Ring buffer next underflow")
            //            throw new RuntimeException("Ring buffer underflow");
        }
        return buffer[indexOut]
    }


}