/*
 * (c) MMXXV Airoku / Claude Sonnet 4.5 All rights reserved for modified part.
 * Modifications:
 * - Removed Android Resources dependency
 * - Replaced R.array references with hardcoded string arrays
 *
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android_baklava.desktop.landroid

import kotlin.random.Random

const val SUFFIX_PROB = 0.75f
const val LETTER_PROB = 0.3f
const val NUMBER_PROB = 0.3f
const val RARE_PROB = 0.05f

class Namer {
    private val planetDescriptors = Bag(arrayOf("rocky", "gaseous", "icy", "volcanic", "barren", "oceanic", "desert", "jungle", "arctic", "temperate"))
    private val lifeDescriptors = Bag(arrayOf("lush", "verdant", "teeming", "thriving", "abundant", "sparse", "rare", "exotic", "primitive", "advanced"))
    private val anyDescriptors = Bag(arrayOf("strange", "mysterious", "unusual", "peculiar", "odd", "curious", "unique", "remarkable", "extraordinary", "fascinating"))
    private val atmoDescriptors = Bag(arrayOf("thin", "thick", "dense", "breathable", "toxic", "corrosive", "methane", "nitrogen", "oxygen-rich", "carbon dioxide"))

    private val planetTypes = Bag(arrayOf("planet", "world", "body", "sphere", "globe", "orb", "dwarf planet", "super-earth", "terrestrial", "giant"))
    private val constellations = Bag(arrayOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega"))
    private val constellationsRare = Bag(arrayOf("Andromeda", "Aquarius", "Aries", "Cancer", "Capricorn", "Cassiopeia", "Cetus", "Draco", "Gemini", "Leo", "Libra", "Orion", "Pegasus", "Perseus", "Pisces", "Sagittarius", "Scorpius", "Taurus", "Ursa Major", "Virgo"))
    private val suffixes = Bag(arrayOf("Prime", "Major", "Minor", "Secundus", "Tertius", "Quartus", "Quintus", "Proxima", "Centralis", "Australis", "Borealis"))
    private val suffixesRare = Bag(arrayOf("Infinitum", "Obscurus", "Terminus", "Ultimata", "Extremis", "Novus", "Antiquus", "Magnus", "Parvus", "Gloriosus"))

    private val planetTable = RandomTable(0.75f to planetDescriptors, 0.25f to anyDescriptors)
    private var lifeTable = RandomTable(0.75f to lifeDescriptors, 0.25f to anyDescriptors)
    private var constellationsTable = RandomTable(RARE_PROB to constellationsRare, 1f - RARE_PROB to constellations)
    private var suffixesTable = RandomTable(RARE_PROB to suffixesRare, 1f - RARE_PROB to suffixes)
    private var atmoTable = RandomTable(0.75f to atmoDescriptors, 0.25f to anyDescriptors)

    private var delimiterTable = RandomTable(15f to " ", 3f to "-", 1f to "_", 1f to "/", 1f to ".", 1f to "*", 1f to "^", 1f to "#", 0.1f to "(^*!%@##!!")

    private var activities = Bag(arrayOf("Collecting {flora}", "Observing {fauna}", "Studying {planet}", "Analyzing {atmo}", "Cataloging specimens", "Taking samples", "Recording data", "Scanning surface", "Deploying probes", "Establishing base camp"))
    
    private var floraGenericPlurals = Bag(arrayOf("plants", "vegetation", "flora", "growths", "organisms", "species", "specimens", "lifeforms"))
    private var faunaGenericPlurals = Bag(arrayOf("creatures", "animals", "fauna", "beings", "organisms", "species", "lifeforms", "entities"))
    private var atmoGenericPlurals = Bag(arrayOf("gases", "particles", "elements", "compounds", "molecules", "vapors", "components"))

    fun describePlanet(rng: Random): String = planetTable.roll(rng).pull(rng) + " " + planetTypes.pull(rng)

    fun describeLife(rng: Random): String = lifeTable.roll(rng).pull(rng)

    fun nameSystem(rng: Random): String {
        val parts = StringBuilder()
        parts.append(constellationsTable.roll(rng).pull(rng))
        if (rng.nextFloat() <= SUFFIX_PROB) {
            parts.append(delimiterTable.roll(rng))
            parts.append(suffixesTable.roll(rng).pull(rng))
            if (rng.nextFloat() <= RARE_PROB) parts.append(' ').append(suffixesRare.pull(rng))
        }
        if (rng.nextFloat() <= LETTER_PROB) {
            parts.append(delimiterTable.roll(rng))
            parts.append('A' + rng.nextInt(0, 26))
            if (rng.nextFloat() <= RARE_PROB) parts.append(delimiterTable.roll(rng))
        }
        if (rng.nextFloat() <= NUMBER_PROB) {
            parts.append(delimiterTable.roll(rng))
            parts.append(rng.nextInt(2, 5039))
        }
        return parts.toString()
    }

    fun describeAtmo(rng: Random): String = atmoTable.roll(rng).pull(rng)
    fun floraPlural(rng: Random): String = floraGenericPlurals.pull(rng)
    fun faunaPlural(rng: Random): String = faunaGenericPlurals.pull(rng)
    fun atmoPlural(rng: Random): String = atmoGenericPlurals.pull(rng)

    val TEMPLATE_REGEX = Regex("""\{(flora|fauna|planet|atmo)\}""")

    fun describeActivity(rng: Random, target: Planet?): String {
        return activities.pull(rng).replace(TEMPLATE_REGEX) {
            when (it.groupValues[1]) {
                "flora" -> (target?.flora ?: "SOME") + " " + floraPlural(rng)
                "fauna" -> (target?.fauna ?: "SOME") + " " + faunaPlural(rng)
                "atmo" -> (target?.atmosphere ?: "SOME") + " " + atmoPlural(rng)
                "planet" -> (target?.description ?: "SOME BODY")
                else -> "unknown template tag: ${it.groupValues[0]}"
            }
        }
    }
}