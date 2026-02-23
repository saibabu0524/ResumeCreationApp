package com.softsuave.resumecreationapp.core.common.extension

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform

/**
 * General-purpose Flow extension functions.
 *
 * Note: Result<T> and asResult() extensions live in core:domain.
 * This file contains only Flow utilities that have no domain dependencies.
 */

/**
 * Emits only distinct values based on a key selector [transform].
 *
 * Unlike [distinctUntilChanged], this compares by the extracted key rather
 * than the value itself, which is useful when you want to deduplicate by
 * a subset of a data class's fields.
 */
inline fun <T, R> Flow<T>.distinctBy(crossinline transform: suspend (T) -> R): Flow<T> =
    transform { value ->
        val key = transform(value)
        var lastKey: R? = null
        if (lastKey != key) {
            lastKey = key
            emit(value)
        }
    }
