package com.getwinharris.bapxcut.utils

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.getwinharris.bapxcut.models.EditOperation
import com.getwinharris.bapxcut.models.EditRecipe
import java.lang.reflect.Type

object ProjectSerializer {

    private val uriSerializer = object : JsonSerializer<Uri>, JsonDeserializer<Uri> {
        override fun serialize(src: Uri, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.toString())
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Uri {
            return Uri.parse(json.asString)
        }
    }

    private val baseGson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Uri::class.java, uriSerializer)
            .create()
    }

    private val editOperationSerializer = object : JsonSerializer<EditOperation>, JsonDeserializer<EditOperation> {
        override fun serialize(src: EditOperation, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val element = baseGson.toJsonTree(src)
            if (element is JsonObject) {
                element.addProperty("type", src.javaClass.simpleName)
                element.addProperty("operationType", src.javaClass.simpleName)
            }
            return element
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EditOperation {
            val jsonObject = json.asJsonObject
            
            var opTypeStr = jsonObject.get("operationType")?.asString
            if (opTypeStr == null) {
                val typeVal = jsonObject.get("type")?.asString
                val validClasses = setOf("Trim", "SpeedMain", "ReverseMain", "MirrorMain", "Crop", "AddText", "Merge", "MaskMain", "MuteAudio", "Transition", "MuteClip", "ColorFilter", "AddBackgroundAudio", "AddImageOverlay", "AddSubtitles", "Adjust", "CanvasBackground")
                
                if (typeVal != null && validClasses.contains(typeVal)) {
                    opTypeStr = typeVal
                } else {
                    if (jsonObject.has("durationMs") && jsonObject.has("index") && !jsonObject.has("items")) {
                        opTypeStr = "Transition"
                    } else if (jsonObject.has("colorHex") && jsonObject.has("blurRadius")) {
                        opTypeStr = "CanvasBackground"
                    } else {
                        opTypeStr = typeVal
                    }
                }
            }
            
            val type = opTypeStr ?: throw JsonParseException("Missing type property in EditOperation")
            
            if (type == "Transition") {
                val t = jsonObject.get("type")?.asString
                if (t != null && t != "Transition") {
                     jsonObject.addProperty("transitionType", t)
                } else if (!jsonObject.has("transitionType")) {
                     jsonObject.addProperty("transitionType", "fade")
                }
            } else if (type == "CanvasBackground") {
                val t = jsonObject.get("type")?.asString
                if (t != null && t != "CanvasBackground") {
                     jsonObject.addProperty("backgroundType", t)
                } else if (!jsonObject.has("backgroundType")) {
                     jsonObject.addProperty("backgroundType", "COLOR")
                }
            }
            
            val clazz = when (type) {
                "Trim" -> EditOperation.Trim::class.java
                "SpeedMain" -> EditOperation.SpeedMain::class.java
                "ReverseMain" -> EditOperation.ReverseMain::class.java
                "MirrorMain" -> EditOperation.MirrorMain::class.java
                "Crop" -> EditOperation.Crop::class.java
                "AddText" -> EditOperation.AddText::class.java
                "Merge" -> EditOperation.Merge::class.java
                "MaskMain" -> EditOperation.MaskMain::class.java
                "MuteAudio" -> EditOperation.MuteAudio::class.java
                "Transition" -> EditOperation.Transition::class.java
                "MuteClip" -> EditOperation.MuteClip::class.java
                "ColorFilter" -> EditOperation.ColorFilter::class.java
                "AddBackgroundAudio" -> EditOperation.AddBackgroundAudio::class.java
                "AddImageOverlay" -> EditOperation.AddImageOverlay::class.java
                "AddSubtitles" -> EditOperation.AddSubtitles::class.java
                "Adjust" -> EditOperation.Adjust::class.java
                "CanvasBackground" -> EditOperation.CanvasBackground::class.java
                else -> null
            }

            if (clazz == null) {
                android.util.Log.w("ProjectSerializer", "Skipping unknown or unsupported EditOperation type: $type")
                return EditOperation.MuteAudio("skipped_unknown_$type")
            }
            
            return try {
                baseGson.fromJson(jsonObject, clazz)
            } catch (e: Exception) {
                android.util.Log.w("ProjectSerializer", "Failed to deserialize operation $type: ${e.message}")
                EditOperation.MuteAudio("skipped_failed_$type")
            }
        }
    }

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, uriSerializer)
        .registerTypeHierarchyAdapter(EditOperation::class.java, editOperationSerializer)
        .create()
        
    fun serialize(recipe: EditRecipe): String {
        return gson.toJson(recipe)
    }
    
    fun deserialize(json: String): EditRecipe {
        val recipe = gson.fromJson(json, EditRecipe::class.java)
        val sanitizedOps = recipe.operations.filterNot { 
            it is EditOperation.MuteAudio && (it.id.startsWith("skipped_unknown_") || it.id.startsWith("skipped_failed_"))
        }
        return recipe.copy(operations = sanitizedOps)
    }

    fun deserialize(reader: java.io.Reader): EditRecipe {
        val recipe = gson.fromJson(reader, EditRecipe::class.java)
        val sanitizedOps = recipe.operations.filterNot { 
            it is EditOperation.MuteAudio && (it.id.startsWith("skipped_unknown_") || it.id.startsWith("skipped_failed_"))
        }
        return recipe.copy(operations = sanitizedOps)
    }
}
