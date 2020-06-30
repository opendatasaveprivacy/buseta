package com.alvinhkh.buseta.kmb.util

import com.alvinhkh.buseta.kmb.model.network.KmbEtaReq
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer

import java.lang.reflect.Type

class KmbEtaSerializer : JsonSerializer<KmbEtaReq> {

    override fun serialize(kmbEtaReq: KmbEtaReq, type: Type, context: JsonSerializationContext): JsonElement {
        val `object` = JsonObject()
        `object`.add("d", context.serialize(kmbEtaReq.d))
        `object`.add("ctr", context.serialize(kmbEtaReq.ctr))
        return `object`
    }

}