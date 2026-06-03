package com.mnebot.riptide.widget

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private const val SNAPSHOT_FILE_NAME = "riptide_widget_snapshot.json"

private object WidgetSnapshotJsonSerializer : Serializer<WidgetSnapshot> {
    override val defaultValue: WidgetSnapshot = WidgetSnapshot.EMPTY

    override suspend fun readFrom(input: InputStream): WidgetSnapshot {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return defaultValue
        return try {
            Json.decodeFromString(WidgetSnapshot.serializer(), bytes.decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read widget snapshot", e)
        }
    }

    override suspend fun writeTo(t: WidgetSnapshot, output: OutputStream) {
        val text = Json.encodeToString(WidgetSnapshot.serializer(), t)
        output.write(text.encodeToByteArray())
    }
}

private val Context.widgetSnapshotDataStore: DataStore<WidgetSnapshot> by dataStore(
    fileName = SNAPSHOT_FILE_NAME,
    serializer = WidgetSnapshotJsonSerializer
)

object WidgetSnapshotStateDefinition : GlanceStateDefinition<WidgetSnapshot> {
    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<WidgetSnapshot> =
        context.applicationContext.widgetSnapshotDataStore

    override fun getLocation(context: Context, fileKey: String): File =
        File(context.applicationContext.filesDir, "datastore/$SNAPSHOT_FILE_NAME")
}
