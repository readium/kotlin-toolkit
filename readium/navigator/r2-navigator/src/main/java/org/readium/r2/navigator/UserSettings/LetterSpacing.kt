package org.readium.r2.navigator.UserSettings

class LetterSpacing(value: Float?){

    val max = 0.5f
    val min = 0.0f
    val step = 0.0625f
    var value = value ?: min

    fun increment(){
        if (value + step <= max){
            value += step
        }
    }

    fun decrement(){
        if (value - step >= min){
            value -= step
        }
    }

    override fun toString() : String {
        return value.toString() + "em"
    }

}