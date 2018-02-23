package org.readium.r2.navigator.UserSettings

class FontSize(value: Int?){

    val max = 300
    val min = 100
    val step = 25
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
        return value.toString() + "%"
    }

}