package com.rmgames.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Test

class Vector3Test {

    @Test
    fun `magnitude of 3-4-5 vector is 5`() {
        assertEquals(5f, Vector3(3f, 4f, 0f).magnitude, 0.0001f)
    }

    @Test
    fun `zero vector has zero magnitude`() {
        assertEquals(0f, Vector3(0f, 0f, 0f).magnitude, 0.0001f)
    }
}
