package com.notegram.util

import kotlin.random.Random

/**
 * Provides random profanity quotes from Monty Python's Holy Grail.
 */
class ProfanityGenerator(
    private val random: Random = Random.Default,
) {

    fun randomQuote(): String = QUOTES.random(random)

    companion object {
        internal val QUOTES = listOf(
            "I blow my nose at you, so-called Arthur-king!",
            "I fart in your general direction!",
            "Your mother was a hamster and your father smelt of elderberries!",
            "Now go away or I shall taunt you a second time!",
            "You empty-headed animal food trough wiper!",
            "I'll wave my private parts at your aunties, you cheesy lot of second-hand electric donkey bottom biters!",
        )
    }
}
