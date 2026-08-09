package com.link.core.presentation.utils

import android.content.Context
import com.link.core.presentation.models.StringWrapper

public fun StringWrapper.asString(context: Context): String {
    return when (this) {
        is StringWrapper.FromString -> value
        is StringWrapper.FromResource -> {
            if (args.isEmpty()) {
                context.getString(resId)
            } else {
                context.getString(resId, *args.toTypedArray())
            }
        }
    }
}


