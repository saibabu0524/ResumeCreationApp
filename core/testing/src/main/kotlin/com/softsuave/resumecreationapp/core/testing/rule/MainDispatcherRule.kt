package com.softsuave.resumecreationapp.core.testing.rule

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 extension that replaces the [Dispatchers.Main] dispatcher with
 * an [UnconfinedTestDispatcher] for the duration of each test.
 *
 * Usage:
 * ```kotlin
 * @ExtendWith(MainDispatcherExtension::class)
 * class MyViewModelTest { ... }
 * ```
 *
 * — OR use the [MainDispatcherRule] JUnit 4 rule wrapper if your tests still
 * use JUnit 4 (some modules may still do).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
