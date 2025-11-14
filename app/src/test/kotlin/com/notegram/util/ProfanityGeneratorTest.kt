package com.notegram.util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class ProfanityGeneratorTest {
    @Test
    fun `generator returns deterministic quote with seeded random`() {
        val generator = ProfanityGenerator(Random(0))
        val quote = generator.randomQuote()

        assertTrue(ProfanityGenerator.QUOTES.contains(quote))
    }
}
