package com.lumenui.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityTargetsTest {

    @Test
    fun everyTargetCarriesAnHttpsHomepage() {
        ConnectivityTargets.all.forEach { target ->
            assertTrue(
                "${target.name} has no homepage",
                target.homepage.startsWith("https://") &&
                    target.homepage.length > "https://".length
            )
        }
    }
}
