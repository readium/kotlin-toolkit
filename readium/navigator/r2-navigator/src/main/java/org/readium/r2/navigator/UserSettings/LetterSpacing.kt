package org.readium.r2.navigator.UserSettings

class LetterSpacing(size: Float?){

    val max = 0.5f
    val min = 0.0f
    val step = 0.0625f
    var size = size ?: 0.0f

    fun increment(){
        if (size + step <= max){
            size += step
        }
    }

    fun decrement(){
        if (size - step >= min){
            size -= step
        }
    }

    override fun toString() : String {
        return size.toString() + "em"
    }

}