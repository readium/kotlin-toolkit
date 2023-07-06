package org.readium.r2.navigator.input

import android.view.KeyEvent as AndroidKeyEvent

/**
 * Represents a keyboard event emitted by a navigator.
 *
 * @param type Nature of the event.
 * @param key Key the user pressed or released.
 * @param modifiers Additional key modifiers for keyboard shortcuts.
 */
data class KeyEvent(
    val type: Type,
    val key: Key,
    val modifiers: KeyModifiers
) {

    enum class Type {
        Down, Up
    }

    constructor(type: Type, event: AndroidKeyEvent) : this(
        type = type,
        key = Key(event),
        modifiers = KeyModifiers(event)
    )
}

@JvmInline
value class KeyModifiers(val value: Int) {

    companion object {
        val None = KeyModifiers(0)
        val Alt = KeyModifiers(1 shl 0)
        val Control = KeyModifiers(1 shl 1)
        val Meta = KeyModifiers(1 shl 2)
        val Shift = KeyModifiers(1 shl 3)

        operator fun invoke(event: AndroidKeyEvent) : KeyModifiers {
            var modifiers = None
            if (event.isAltPressed) {
                modifiers += Alt
            }
            if (event.isCtrlPressed) {
                modifiers += Control
            }
            if (event.isMetaPressed) {
                modifiers += Meta
            }
            if (event.isShiftPressed) {
                modifiers += Shift
            }
            return modifiers
        }
    }

    fun contains(other: KeyModifiers): Boolean =
        (value and other.value) == other.value

    operator fun plus(other: KeyModifiers): KeyModifiers =
        KeyModifiers(value or other.value)
}

@JvmInline
value class Key(val code: String) {

    companion object {
        val Unknown = Key("")

        val Backspace = Key("Backspace")
        val Enter = Key("Enter")
        val ForwardDelete = Key("ForwardDelete")
        val Space = Key("Space")
        val Tab = Key("Tab")

        val Digit0 = Key("Digit0")
        val Digit1 = Key("Digit1")
        val Digit2 = Key("Digit2")
        val Digit3 = Key("Digit3")
        val Digit4 = Key("Digit4")
        val Digit5 = Key("Digit5")
        val Digit6 = Key("Digit6")
        val Digit7 = Key("Digit7")
        val Digit8 = Key("Digit8")
        val Digit9 = Key("Digit9")

        val A = Key("A")
        val B = Key("B")
        val C = Key("C")
        val D = Key("D")
        val E = Key("E")
        val F = Key("F")
        val G = Key("G")
        val H = Key("H")
        val I = Key("I")
        val J = Key("J")
        val K = Key("K")
        val L = Key("L")
        val M = Key("M")
        val N = Key("N")
        val O = Key("O")
        val P = Key("P")
        val Q = Key("Q")
        val R = Key("R")
        val S = Key("S")
        val T = Key("T")
        val U = Key("U")
        val V = Key("V")
        val W = Key("W")
        val X = Key("X")
        val Y = Key("Y")
        val Z = Key("Z")

        val Apostrophe = Key("Apostrophe")
        val At = Key("At")
        val Backslash = Key("Backslash")
        val Backtick = Key("Backtick")
        val Comma = Key("Comma")
        val Equals = Key("Equals")
        val LeftBracket = Key("LeftBracket")
        val Minus = Key("Minus")
        val Period = Key("Period")
        val Plus = Key("Plus")
        val Pound = Key("Pound")
        val RightBracket = Key("RightBracket")
        val Semicolon = Key("Semicolon")
        val Slash = Key("Slash")
        val Star = Key("Star")

        val NumLock = Key("NumLock")
        val Numpad0 = Key("Numpad0")
        val Numpad1 = Key("Numpad1")
        val Numpad2 = Key("Numpad2")
        val Numpad3 = Key("Numpad3")
        val Numpad4 = Key("Numpad4")
        val Numpad5 = Key("Numpad5")
        val Numpad6 = Key("Numpad6")
        val Numpad7 = Key("Numpad7")
        val Numpad8 = Key("Numpad8")
        val Numpad9 = Key("Numpad9")
        val NumpadAdd = Key("NumpadAdd")
        val NumpadComma = Key("NumpadComma")
        val NumpadDivide = Key("NumpadDivide")
        val NumpadDot = Key("NumpadDot")
        val NumpadEnter = Key("NumpadEnter")
        val NumpadEquals = Key("NumpadEquals")
        val NumpadLeftParen = Key("NumpadLeftParen")
        val NumpadMultiply = Key("NumpadMultiply")
        val NumpadRightParen = Key("NumpadRightParen")
        val NumpadSubtract = Key("NumpadSubtract")

        val CapsLock = Key("CapsLock")
        val Escape = Key("Escape")
        val Function = Key("Function")
        val Insert = Key("Insert")

        val ArrowDown = Key("ArrowDown")
        val ArrowLeft = Key("ArrowLeft")
        val ArrowRight = Key("ArrowRight")
        val ArrowUp = Key("ArrowUp")
        val End = Key("End")
        val Home = Key("Home")
        val PageDown = Key("PageDown")
        val PageUp = Key("PageUp")

        val F1 = Key("F1")
        val F2 = Key("F2")
        val F3 = Key("F3")
        val F4 = Key("F4")
        val F5 = Key("F5")
        val F6 = Key("F6")
        val F7 = Key("F7")
        val F8 = Key("F8")
        val F9 = Key("F9")
        val F10 = Key("F10")
        val F11 = Key("F11")
        val F12 = Key("F12")

        val BrightnessDown = Key("BrightnessDown")
        val BrightnessUp = Key("BrightnessUp")
        val MediaFastForward = Key("MediaFastForward")
        val MediaNext = Key("MediaNext")
        val MediaPause = Key("MediaPause")
        val MediaPlay = Key("MediaPlay")
        val MediaPlayPause = Key("MediaPlayPause")
        val MediaPrevious = Key("MediaPrevious")
        val MediaRewind = Key("MediaRewind")
        val MediaSkipBackward = Key("MediaSkipBackward")
        val MediaSkipForward = Key("MediaSkipForward")
        val MediaStop = Key("MediaStop")
        val VolumeDown = Key("VolumeDown")
        val VolumeMute = Key("VolumeMute")
        val VolumeUp = Key("VolumeUp")
        val ZoomIn = Key("ZoomIn")
        val ZoomOut = Key("ZoomOut")

        val AltLeft = Key("AltLeft")
        val AltRight = Key("AltRight")
        val ControlLeft = Key("ControlLeft")
        val ControlRight = Key("ControlRight")
        val MetaLeft = Key("MetaLeft")
        val MetaRight = Key("MetaRight")
        val ShiftLeft = Key("ShiftLeft")
        val ShiftRight = Key("ShiftRight")

        operator fun invoke(event: AndroidKeyEvent) : Key =
            when (event.keyCode) {
                AndroidKeyEvent.KEYCODE_DEL -> Backspace
                AndroidKeyEvent.KEYCODE_ENTER -> Enter
                AndroidKeyEvent.KEYCODE_FORWARD_DEL -> ForwardDelete
                AndroidKeyEvent.KEYCODE_SPACE -> Space
                AndroidKeyEvent.KEYCODE_TAB -> Tab

                AndroidKeyEvent.KEYCODE_0 -> Digit0
                AndroidKeyEvent.KEYCODE_1 -> Digit1
                AndroidKeyEvent.KEYCODE_2 -> Digit2
                AndroidKeyEvent.KEYCODE_3 -> Digit3
                AndroidKeyEvent.KEYCODE_4 -> Digit4
                AndroidKeyEvent.KEYCODE_5 -> Digit5
                AndroidKeyEvent.KEYCODE_6 -> Digit6
                AndroidKeyEvent.KEYCODE_7 -> Digit7
                AndroidKeyEvent.KEYCODE_8 -> Digit8
                AndroidKeyEvent.KEYCODE_9 -> Digit9

                AndroidKeyEvent.KEYCODE_A -> A
                AndroidKeyEvent.KEYCODE_B -> B
                AndroidKeyEvent.KEYCODE_C -> C
                AndroidKeyEvent.KEYCODE_D -> D
                AndroidKeyEvent.KEYCODE_E -> E
                AndroidKeyEvent.KEYCODE_F -> F
                AndroidKeyEvent.KEYCODE_G -> G
                AndroidKeyEvent.KEYCODE_H -> H
                AndroidKeyEvent.KEYCODE_I -> I
                AndroidKeyEvent.KEYCODE_J -> J
                AndroidKeyEvent.KEYCODE_K -> K
                AndroidKeyEvent.KEYCODE_L -> L
                AndroidKeyEvent.KEYCODE_M -> M
                AndroidKeyEvent.KEYCODE_N -> N
                AndroidKeyEvent.KEYCODE_O -> O
                AndroidKeyEvent.KEYCODE_P -> P
                AndroidKeyEvent.KEYCODE_Q -> Q
                AndroidKeyEvent.KEYCODE_R -> R
                AndroidKeyEvent.KEYCODE_S -> S
                AndroidKeyEvent.KEYCODE_T -> T
                AndroidKeyEvent.KEYCODE_U -> U
                AndroidKeyEvent.KEYCODE_V -> V
                AndroidKeyEvent.KEYCODE_W -> W
                AndroidKeyEvent.KEYCODE_X -> X
                AndroidKeyEvent.KEYCODE_Y -> Y
                AndroidKeyEvent.KEYCODE_Z -> Z

                AndroidKeyEvent.KEYCODE_APOSTROPHE -> Apostrophe
                AndroidKeyEvent.KEYCODE_AT -> At
                AndroidKeyEvent.KEYCODE_BACKSLASH -> Backslash
                AndroidKeyEvent.KEYCODE_GRAVE -> Backtick
                AndroidKeyEvent.KEYCODE_COMMA -> Comma
                AndroidKeyEvent.KEYCODE_EQUALS -> Equals
                AndroidKeyEvent.KEYCODE_LEFT_BRACKET -> LeftBracket
                AndroidKeyEvent.KEYCODE_MINUS -> Minus
                AndroidKeyEvent.KEYCODE_PERIOD -> Period
                AndroidKeyEvent.KEYCODE_PLUS -> Plus
                AndroidKeyEvent.KEYCODE_POUND -> Pound
                AndroidKeyEvent.KEYCODE_RIGHT_BRACKET -> RightBracket
                AndroidKeyEvent.KEYCODE_SEMICOLON -> Semicolon
                AndroidKeyEvent.KEYCODE_SLASH -> Slash
                AndroidKeyEvent.KEYCODE_STAR -> Star

                AndroidKeyEvent.KEYCODE_NUM_LOCK -> NumLock
                AndroidKeyEvent.KEYCODE_NUMPAD_0 -> Numpad0
                AndroidKeyEvent.KEYCODE_NUMPAD_1 -> Numpad1
                AndroidKeyEvent.KEYCODE_NUMPAD_2 -> Numpad2
                AndroidKeyEvent.KEYCODE_NUMPAD_3 -> Numpad3
                AndroidKeyEvent.KEYCODE_NUMPAD_4 -> Numpad4
                AndroidKeyEvent.KEYCODE_NUMPAD_5 -> Numpad5
                AndroidKeyEvent.KEYCODE_NUMPAD_6 -> Numpad6
                AndroidKeyEvent.KEYCODE_NUMPAD_7 -> Numpad7
                AndroidKeyEvent.KEYCODE_NUMPAD_8 -> Numpad8
                AndroidKeyEvent.KEYCODE_NUMPAD_9 -> Numpad9
                AndroidKeyEvent.KEYCODE_NUMPAD_ADD -> NumpadAdd
                AndroidKeyEvent.KEYCODE_NUMPAD_COMMA -> NumpadComma
                AndroidKeyEvent.KEYCODE_NUMPAD_DIVIDE -> NumpadDivide
                AndroidKeyEvent.KEYCODE_NUMPAD_DOT -> NumpadDot
                AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> NumpadEnter
                AndroidKeyEvent.KEYCODE_NUMPAD_EQUALS -> NumpadEquals
                AndroidKeyEvent.KEYCODE_NUMPAD_LEFT_PAREN -> NumpadLeftParen
                AndroidKeyEvent.KEYCODE_NUMPAD_MULTIPLY -> NumpadMultiply
                AndroidKeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN -> NumpadRightParen
                AndroidKeyEvent.KEYCODE_NUMPAD_SUBTRACT -> NumpadSubtract

                AndroidKeyEvent.KEYCODE_CAPS_LOCK -> CapsLock
                AndroidKeyEvent.KEYCODE_ESCAPE -> Escape
                AndroidKeyEvent.KEYCODE_FUNCTION -> Function
                AndroidKeyEvent.KEYCODE_INSERT -> Insert

                AndroidKeyEvent.KEYCODE_DPAD_DOWN -> ArrowDown
                AndroidKeyEvent.KEYCODE_DPAD_LEFT -> ArrowLeft
                AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> ArrowRight
                AndroidKeyEvent.KEYCODE_DPAD_UP -> ArrowUp
                AndroidKeyEvent.KEYCODE_HOME -> Home
                AndroidKeyEvent.KEYCODE_PAGE_DOWN -> PageDown
                AndroidKeyEvent.KEYCODE_PAGE_UP -> PageUp

                AndroidKeyEvent.KEYCODE_F1 -> F1
                AndroidKeyEvent.KEYCODE_F2 -> F2
                AndroidKeyEvent.KEYCODE_F3 -> F3
                AndroidKeyEvent.KEYCODE_F4 -> F4
                AndroidKeyEvent.KEYCODE_F5 -> F5
                AndroidKeyEvent.KEYCODE_F6 -> F6
                AndroidKeyEvent.KEYCODE_F7 -> F7
                AndroidKeyEvent.KEYCODE_F8 -> F8
                AndroidKeyEvent.KEYCODE_F9 -> F9
                AndroidKeyEvent.KEYCODE_F10 -> F10
                AndroidKeyEvent.KEYCODE_F11 -> F11
                AndroidKeyEvent.KEYCODE_F12 -> F12

                AndroidKeyEvent.KEYCODE_BRIGHTNESS_DOWN -> BrightnessDown
                AndroidKeyEvent.KEYCODE_BRIGHTNESS_UP -> BrightnessUp
                AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> MediaFastForward
                AndroidKeyEvent.KEYCODE_MEDIA_NEXT -> MediaNext
                AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> MediaPause
                AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> MediaPlay
                AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> MediaPlayPause
                AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaPrevious
                AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> MediaRewind
                AndroidKeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> MediaSkipBackward
                AndroidKeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> MediaSkipForward
                AndroidKeyEvent.KEYCODE_MEDIA_STOP -> MediaStop
                AndroidKeyEvent.KEYCODE_VOLUME_DOWN -> VolumeDown
                AndroidKeyEvent.KEYCODE_VOLUME_MUTE -> VolumeMute
                AndroidKeyEvent.KEYCODE_VOLUME_UP -> VolumeUp
                AndroidKeyEvent.KEYCODE_ZOOM_IN -> ZoomIn
                AndroidKeyEvent.KEYCODE_ZOOM_OUT -> ZoomOut

                AndroidKeyEvent.KEYCODE_ALT_LEFT -> AltLeft
                AndroidKeyEvent.KEYCODE_ALT_RIGHT -> AltRight
                AndroidKeyEvent.KEYCODE_CTRL_LEFT -> ControlLeft
                AndroidKeyEvent.KEYCODE_CTRL_RIGHT -> ControlRight
                AndroidKeyEvent.KEYCODE_META_LEFT -> MetaLeft
                AndroidKeyEvent.KEYCODE_META_RIGHT -> MetaRight
                AndroidKeyEvent.KEYCODE_SHIFT_LEFT -> ShiftLeft
                AndroidKeyEvent.KEYCODE_SHIFT_RIGHT -> ShiftRight

                else -> Unknown
            }
    }
}
