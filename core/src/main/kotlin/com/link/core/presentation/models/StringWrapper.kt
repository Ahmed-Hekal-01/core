package com.link.core.presentation.models

public sealed class StringWrapper {
    public data class FromString(public val value: String) : StringWrapper()
    public data class FromResource(public val resId: Int, public val args: List<Any> = emptyList()) : StringWrapper()
}
