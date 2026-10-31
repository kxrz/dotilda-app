package com.dotilda.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every translated question bank and journey file mirrors the English one: same questions,
 * same ids, types, tags and Mirror groups, two options for each This or that. Only the words
 * change, so entries, Mirror questions and journeys keep working in every language.
 */
class TranslationsTest {
    private val assets = File("src/main/assets")
    private val json = Json

    private fun read(name: String) = json.parseToJsonElement(File(assets, name).readText()).jsonObject

    private fun translations(base: String) =
        assets.listFiles().orEmpty().map { it.name }.filter { it.startsWith("$base-") && it.endsWith(".json") }

    private fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.content

    private fun assertMirrors(english: JsonArray, translated: JsonArray, file: String) {
        assertEquals("$file: number of questions", english.size, translated.size)
        english.zip(translated).forEach { (en, tr) ->
            val e = en.jsonObject
            val t = tr.jsonObject
            val id = e.str("id")
            assertEquals("$file: id", id, t.str("id"))
            assertEquals("$file $id: type", e.str("type"), t.str("type"))
            assertEquals("$file $id: tags", e["tags"], t["tags"])
            assertEquals("$file $id: mirrorGroup", e.str("mirrorGroup"), t.str("mirrorGroup"))
            assertTrue("$file $id: text", !t.str("text").isNullOrBlank())
            if (e.str("type") == "this_or_that") assertEquals("$file $id: options", 2, t["options"]?.jsonArray?.size)
        }
    }

    @Test
    fun questionBanksMirrorEnglish() {
        val english = read("questions.json")["questions"]!!.jsonArray
        translations("questions").forEach { assertMirrors(english, read(it)["questions"]!!.jsonArray, it) }
    }

    @Test
    fun journeysMirrorEnglish() {
        val english = read("journeys.json")["journeys"]!!.jsonArray
        translations("journeys").forEach { file ->
            val translated = read(file)["journeys"]!!.jsonArray
            assertEquals("$file: number of journeys", english.size, translated.size)
            english.zip(translated).forEach { (en, tr) ->
                assertEquals("$file: journey id", en.jsonObject.str("id"), tr.jsonObject.str("id"))
                assertTrue("$file: constellation name", !tr.jsonObject.str("constellation").isNullOrBlank())
                assertMirrors(en.jsonObject["questions"]!!.jsonArray, tr.jsonObject["questions"]!!.jsonArray, file)
            }
        }
    }
}
