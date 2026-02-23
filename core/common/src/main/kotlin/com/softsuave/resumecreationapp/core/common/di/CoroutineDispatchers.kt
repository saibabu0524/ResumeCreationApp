package com.softsuave.resumecreationapp.core.common.di

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY

/**
 * Qualifier for [kotlinx.coroutines.Dispatchers.IO].
 *
 * Use this instead of hardcoding `Dispatchers.IO` anywhere in production code.
 *
 * ```kotlin
 * class MyRepository @Inject constructor(
 *     @IoDispatcher private val dispatcher: CoroutineDispatcher
 * )
 * ```
 */
@Qualifier
@Retention(BINARY)
annotation class IoDispatcher

/**
 * Qualifier for [kotlinx.coroutines.Dispatchers.Main].
 */
@Qualifier
@Retention(BINARY)
annotation class MainDispatcher

/**
 * Qualifier for [kotlinx.coroutines.Dispatchers.Default].
 */
@Qualifier
@Retention(BINARY)
annotation class DefaultDispatcher
