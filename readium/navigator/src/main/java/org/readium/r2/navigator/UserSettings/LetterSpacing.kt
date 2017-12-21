package org.readium.r2.navigator.UserSettings

class LetterSpacing(size: Double?){

    val max = 0.5
    val min = 0.1
    val step = 0.125
    var size = size ?: 0.0

    fun increment(){
        if (size + step < max){
            size += step
        }
    }

    fun decrement(){
        if (size - step > min){
            size -= step
        }
    }

    override fun toString() : String {
        return size.toString() + "em"
    }

}