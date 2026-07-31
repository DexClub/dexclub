package io.github.dexclub.mcp

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class McpFindQuerySchemaTest {
    @Test
    fun catalogExposesOnlyUnifiedFindTools() {
        val findNames = McpDexToolCatalog.tools.map { it.name }.filter { it.startsWith("find_") }

        assertEquals(listOf("find_classes", "find_methods", "find_fields"), findNames)
    }

    @Test
    fun findQuerySchemasUseStrictRecursiveDefinitions() {
        listOf("find_classes", "find_methods", "find_fields").forEach { toolName ->
            val metadata = McpDexToolCatalog.require(toolName)
            val schema = metadata.toToolSchema()
            val query = schema.properties!!.getValue("query").jsonObject
            val rootName = query.getValue("\$ref").jsonPrimitive.content.substringAfterLast('/')
            val root = assertNotNull(schema.defs)[rootName]!!.jsonObject
            val properties = root.getValue("properties").jsonObject

            assertEquals(setOf("query"), metadata.required)
            assertEquals(false, root.getValue("additionalProperties").jsonPrimitive.content.toBoolean())
            assertFalse("searchInClasses" in properties)
            assertFalse("searchInMethods" in properties)
            assertFalse("searchInFields" in properties)
            assertTrue(schema.defs!!.size > 1)
        }
    }
}
