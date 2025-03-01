package com.angorasix.projects.management.integrations.integration.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.bson.Document
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import java.io.File
import java.text.SimpleDateFormat

/**
 *
 *
 * @author rozagerardo
 */

const val DATE_TIME = "dateTime"
const val CREATED_AT = "createdAt"

private fun mapCreatedAt(fieldsMap: MutableMap<String, Any>): Map<String, Any> {
    fieldsMap[DATE_TIME] =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(fieldsMap[DATE_TIME] as String)
    return fieldsMap
}

suspend fun initializeMongodb(
    jsonFile: String,
    template: ReactiveMongoTemplate,
    mapper: ObjectMapper,
) {
    val file: File = ClassPathResource(jsonFile).file
    val dataEntries: Collection<MutableMap<String, Any>> = mapper.readValue(file.inputStream())

    dataEntries
        .asFlow()
        .map { entry ->
            if (entry.containsKey(CREATED_AT)) {
                entry[CREATED_AT] =
                    mapCreatedAt(@Suppress("UNCHECKED_CAST") (entry[CREATED_AT] as MutableMap<String, Any>))
            }
            val document = Document(entry)
            template
                .insert(
                    document,
                    "projectManagement",
                ).block()
        }.collect()
}
