package com.alvinhkh.buseta.kmb.model.network

import com.google.gson.annotations.SerializedName
import java.util.*

data class KmbStimeRes(
        @SerializedName("stime")
        var stime: Date? = null
)