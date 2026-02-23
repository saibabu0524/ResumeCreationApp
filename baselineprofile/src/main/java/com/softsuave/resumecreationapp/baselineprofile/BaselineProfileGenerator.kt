package com.softsuave.resumecreationapp.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generate() = baselineRule.collect(
        packageName = "com.softsuave.resumecreationapp",
        profileBlock = {
            // Start the app for the baseline profile
            startActivityAndWait()
            
            // Wait for main screen content
            device.wait(Until.hasObject(By.res("home_list")), 5000)
            
            // Perform scroll interaction on the home screen list
            val list = device.findObject(By.res("home_list"))
            if (list != null) {
                list.setGestureMargin(device.displayWidth / 5)
                list.scroll(Direction.DOWN, 1f)
                list.scroll(Direction.UP, 1f)
            }
        }
    )
}
