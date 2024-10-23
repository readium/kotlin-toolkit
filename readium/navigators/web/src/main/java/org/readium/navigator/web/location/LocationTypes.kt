package org.readium.navigator.web.location

import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.Location
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
@JvmInline
public value class CssSelector(
    public val value: String
)

@ExperimentalReadiumApi
@JvmInline
public value class Progression(
    public val value: Double
)

@ExperimentalReadiumApi
@JvmInline
public value class TextFragment(
    public val value: String
)

@ExperimentalReadiumApi
public sealed interface FixedWebGoLocation : GoLocation

@ExperimentalReadiumApi
public sealed interface ReflowableWebGoLocation : GoLocation

@ExperimentalReadiumApi
public data class ProgressionLocation(
    val href: Url,
    val progression: Double
) : ReflowableWebGoLocation

@ExperimentalReadiumApi
public data class TextLocation(
    val href: Url,
    val cssSelector: String?,
    val textBefore: String?,
    val textAfter: String?
) : ReflowableWebGoLocation

@ExperimentalReadiumApi
public data class PositionLocation(
    val position: Int
) : ReflowableWebGoLocation

@ExperimentalReadiumApi
public data class HrefLocation(
    val href: Url
) : ReflowableWebGoLocation, FixedWebGoLocation

@ExperimentalReadiumApi
public data class ReflowableWebLocation(
    override val href: Url,
    val progression: Double,
    val cssSelector: String?,
    val textBefore: String?,
    val textAfter: String?,
    val position: Int?
) : Location

@ExperimentalReadiumApi
public data class FixedWebLocation(
    override val href: Url
) : Location
