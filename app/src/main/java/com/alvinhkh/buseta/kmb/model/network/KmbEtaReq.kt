package com.alvinhkh.buseta.kmb.model.network

import com.google.gson.annotations.SerializedName


data class KmbEtaReq(
        @SerializedName("d")
        var d: String,

        @SerializedName("ctr")
        var ctr: String
)