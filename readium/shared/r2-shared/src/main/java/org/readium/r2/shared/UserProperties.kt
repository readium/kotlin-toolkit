/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

package org.readium.r2.shared

import java.io.Serializable

sealed class UserProperty(var ref: String, var name: String) {

    private var value: String = ""
    get() = this.toString()
    abstract override fun toString(): String
    fun getJson(): String {
        return """{name:"$name",value:"${this}"}"""
    }

}


// TODO add here your new Subclasses of UserPreference. It has to be an abstract class inheriting from UserSetting.

class Enumeratable(var index: Int, private val values: List<String>, ref: String, name: String) :
        UserProperty(ref, name) {

    override fun toString() = values[index]
}

class Incrementable(var value: Float,
                    val min: Float,
                    val max: Float,
                    val step: Float,
                    private val suffix: String,
                    ref: String,
                    name: String) :
        UserProperty(ref, name) {

    fun increment() {
        value +=  (if (value + step <= max) step else 0.0f)
    }

    fun decrement() {
        value -= (if (value - step >= min) step else 0.0f)
    }

    override fun toString() = value.toString() + suffix
}

class Switchable(onValue: String, offValue: String, var on: Boolean,
        ref: String, name: String) :
        UserProperty(ref, name) {

    private val values = mapOf(true to onValue, false to offValue)

    fun switch() {
        on = !on
    }

    override fun toString() = values[on]!!
}


class UserProperties : Serializable {

    val properties: MutableList<UserProperty> = mutableListOf()

    fun addIncrementable(nValue: Float, min: Float, max: Float, step: Float, suffix: String, ref: String, name: String) {
        properties.add(Incrementable(nValue, min, max, step, suffix, ref, name))
    }

    fun addSwitchable(onValue: String, offValue: String, on: Boolean, ref: String, name: String) {
        properties.add(Switchable(onValue, offValue, on, ref, name))
    }

    fun addEnumeratable(index: Int, values: List<String>, ref: String, name: String) {
        properties.add(Enumeratable(index, values, ref, name))
    }

    fun <T : UserProperty>getByRef(ref: String) = properties.filter {
        it.ref == ref
    }.firstOrNull()!! as T
}

