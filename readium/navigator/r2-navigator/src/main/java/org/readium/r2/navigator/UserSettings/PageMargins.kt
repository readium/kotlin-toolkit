package org.readium.r2.navigator.UserSettings

class PageMargins(size: Float?){

    val max = 2.0f
    val min = 0.5f
    val step = 0.25f
    var size = size ?: 0.5f

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
        return size.toString() + "rem"
    }

}