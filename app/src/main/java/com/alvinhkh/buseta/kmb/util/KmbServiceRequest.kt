package com.alvinhkh.buseta.kmb.util

import android.content.Context

import com.alvinhkh.buseta.kmb.KmbService
import com.alvinhkh.buseta.kmb.model.network.KmbEtaReq
import com.alvinhkh.buseta.kmb.model.network.KmbEtaRes
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

import org.json.JSONException

import timber.log.Timber


class KmbServiceRequest(context: Context, private val kmbService: KmbService) {

    class KmbEtaSpec(internal var reqRoute: String? // route
                     , internal var reqBound: String? // bound
                     , internal var reqSequence: String?  // stop sequence
                     , internal var reqServiceType: String? // service type
    ) {
        internal var lastUpdated = -1L  // updated
        internal var singleRetries = 0 // retries count
    }

    private object KmbParams {
        internal var batchRetries = 0
        internal var apiKey: String? = ""
        internal var ivLong: Long = -1
        internal var adjustMillis = 0L
        internal var aligned = false
        // In place of android id, server accepts empty
        val devId: String
            get() = ""

    }

    init {
        if (!KmbParams.aligned) {
            try {
                alignTime()
            } catch (e: Exception) {
                Timber.e("Align time error", e)
            }

        }
    }


    @JvmOverloads
    fun getEta(etaSpec: KmbEtaSpec, lang: String, update: Boolean = false): Array<KmbEtaRes>? {
        try {
            var str = etaSpec.reqServiceType?:"1"  // service type
            if (str.startsWith("0")) {
                str = str.substring(1)
            }

            val query = StringBuilder("?lang=")
                    .append(lang)
                    .append("&route=")
                    .append(etaSpec.reqRoute)
                    .append("&bound=")
                    .append(etaSpec.reqBound)
                    .append("&stop_seq=")
                    .append(etaSpec.reqSequence)
                    .append("&service_type=")
                    .append(str)
            if (update) {
                query.append("&updated=")
                        .append(etaSpec.lastUpdated)
            }

            return KmbServiceTask(kmbService, query.toString(), etaSpec).run()

        } catch (e: Exception) {
            Timber.e("getEtaUpdate error", e)
        }

        return null
    }

    @Throws(IOException::class)
    private fun alignTime() {
        val kmbServiceDataFeed = KmbService.etadatafeed.create(KmbService::class.java)
        try {
            val response = kmbServiceDataFeed.etaSTime().execute()
            if (response.isSuccessful) {
                val stimes = response.body()
                if (stimes!!.size > 0) {
                    val stime = stimes[0].stime
                    if (stime != null) {
                        KmbParams.adjustMillis = stime.time - System.currentTimeMillis()
                        Timber.d("aligned time: " + KmbParams.adjustMillis)
                        KmbParams.aligned = true
                    } else {
                        Timber.e("alignTime: stime not found")
                    }
                } else {
                    Timber.e("alignTime: empty return")
                }
            } else {
                Timber.e("alignTime: Error: " + response.code() + " Message: " + response.message())
            }
        } catch (e: Exception) {
            Timber.e("alignTime: Error in request", e)
        }

    }

    class KmbServiceTask {

        private val singleSpec: KmbEtaSpec?
        private val multiSpec: ArrayList<KmbEtaSpec>?
        private var kmbService: KmbService

        private var query: String
        private var isRetry: Boolean = false

        private var responseCode: Int = 0

        private var ivLong = -1L
        private var apiKey: String? = ""


        internal constructor(kmbService: KmbService, query: String, etaSpec: KmbEtaSpec) : this(kmbService, query, etaSpec, false)

        internal constructor(kmbService: KmbService, query: String, etaSpecList: ArrayList<KmbEtaSpec>) : this(kmbService, query, etaSpecList, false)


        private constructor(kmbService: KmbService, query: String, etaSpec: KmbEtaSpec, isRetry: Boolean) {
            this.kmbService = kmbService
            this.query = query
            this.singleSpec = etaSpec
            this.multiSpec = null
            this.isRetry = isRetry
        }

        private constructor(kmbService: KmbService, query: String, etaSpecList: ArrayList<KmbEtaSpec>, isRetry: Boolean) {
            this.kmbService = kmbService
            this.query = query
            this.singleSpec = null
            this.multiSpec = ArrayList(etaSpecList)
            this.isRetry = isRetry
        }

        fun run(): Array<KmbEtaRes>? {
            // Do the job
            val result: Array<KmbEtaRes>?
            try {
                result = this.executeReq()
            } catch (e: Exception) {
                Timber.e("Eta Request exception", e)
                return null
            }

            return retryOrReturn(result)
        }

        @Throws(IOException::class, JSONException::class)
        private fun executeReq(): Array<KmbEtaRes>? {
            this.responseCode = 0

            if (!isRetry && KmbParams.apiKey != "" && KmbParams.ivLong != -1L) {
                apiKey = KmbParams.apiKey
                ivLong = KmbParams.ivLong
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val now = Calendar.getInstance()
                now.timeInMillis = System.currentTimeMillis() + KmbParams.adjustMillis
                val encodedMsg = KmbServiceEncodeUtil.encodeKmb(sdf.format(now.time), null)
                apiKey = encodedMsg.encodedString
                ivLong = encodedMsg.ivLong
            }

            val keyParam = StringBuilder("&apiKey=")
                    .append(apiKey)
                    .append("&ctr=")
                    .append(ivLong)
                    .toString()

            if (!query.contains("&vendor_id=")) {
                val utfSb = StringBuilder()
                        .append(query)
                        .append("&vendor_id=")
                        .append(KmbParams.devId)
                query = utfSb.toString()
            }

            val encodedParams = KmbServiceEncodeUtil.encodeKmb(query + keyParam, ivLong).encodedString

            try {
                val response = kmbService.eta(KmbEtaReq(encodedParams!!, ivLong.toString())).execute()
                this.responseCode = response.code()
                if (response.isSuccessful) {
                    return response.body()
                } else {
                    Timber.e("Error: " + response.code() + " Message: " + response.message())
                }
            } catch (e: Exception) {
                Timber.e("Error in eta request: " + e.message, e)
            }

            return null
        }

        private fun retryOrReturn(result: Array<KmbEtaRes>?): Array<KmbEtaRes>? {
            // Response code 403 = client forbidden
            if (responseCode == 403) {
                if (this.singleSpec != null) {
                    // single
                    this.singleSpec.singleRetries++
                    if (this.singleSpec.singleRetries < 3) {
                        try {
                            Timber.w("Request retry : " + singleSpec.singleRetries)
                            return KmbServiceTask(kmbService, query, singleSpec, true).run()
                        } catch (e: Exception) {
                            Timber.e("Retry failed", e)
                        }

                    }
                } else if (multiSpec != null) {
                    // array
                    val i = KmbParams.batchRetries + 1
                    KmbParams.batchRetries = i
                    if (i < 3) {
                        try {
                            Timber.w("Batch request retry : $i")
                            return KmbServiceTask(kmbService, query, multiSpec, true).run()
                        } catch (e2: Exception) {
                            Timber.e("Retry multi spec failed", e2)
                        }

                    } else {
                        KmbParams.batchRetries = 0
                    }
                }

            } else if (responseCode in 200..299) {
                // Server validated, save for reuse
                KmbParams.apiKey = apiKey
                KmbParams.ivLong = ivLong
                return result
            }
            return null

        }
    }

}