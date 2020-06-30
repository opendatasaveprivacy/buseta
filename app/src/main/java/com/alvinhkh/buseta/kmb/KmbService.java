package com.alvinhkh.buseta.kmb;

import com.alvinhkh.buseta.App;
import com.alvinhkh.buseta.kmb.model.KmbEtaRoutes;
import com.alvinhkh.buseta.kmb.model.KmbRoutesInStop;
import com.alvinhkh.buseta.kmb.model.network.KmbAnnounceRes;
import com.alvinhkh.buseta.kmb.model.KmbBBI2;
import com.alvinhkh.buseta.kmb.model.network.KmbEtaReq;
import com.alvinhkh.buseta.kmb.model.network.KmbEtaRes;
import com.alvinhkh.buseta.kmb.model.network.KmbRouteBoundRes;
import com.alvinhkh.buseta.kmb.model.network.KmbScheduleRes;
import com.alvinhkh.buseta.kmb.model.network.KmbSpecialRouteRes;
import com.alvinhkh.buseta.kmb.model.network.KmbStimeRes;
import com.alvinhkh.buseta.kmb.model.network.KmbStopsRes;
import com.alvinhkh.buseta.kmb.util.DateDeserializer;
import com.alvinhkh.buseta.kmb.util.KmbEtaSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory;

import java.util.Date;
import java.util.List;

import kotlinx.coroutines.Deferred;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface KmbService {

    String ANNOUNCEMENT_PICTURE = "http://search.kmb.hk/KMBWebSite/AnnouncementPicture.ashx?url=";

    Gson gson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(KmbEtaReq.class, new KmbEtaSerializer())
            .create();

    Retrofit webCoroutine = new Retrofit.Builder()
            .client(App.Companion.getHttpClient())
            .baseUrl("http://www.kmb.hk/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory.create())
            .build();

    @GET("ajax/BBI/get_BBI2.php?&buscompany=null&jtSorting=sec_routeno%20ASC")
    Deferred<Response<KmbBBI2>> bbi(@Query("routeno") String routeno, @Query("bound") String bound);

    @GET("ajax/BBI/get_BBI2.php?&buscompany=null&jtSorting=sec_routeno%20ASC")
    Deferred<Response<KmbBBI2>> bbi(@Query("routeno") String routeno, @Query("bound") String bound, @Query("interchangeType") String interchangeType);

    Retrofit webSearch = new Retrofit.Builder()
            .client(App.Companion.getHttpClient())
            .baseUrl("http://search.kmb.hk/KMBWebSite/Function/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    @GET("FunctionRequest.ashx?action=getRouteBound")
    Call<KmbRouteBoundRes> routeBound(@Query("route") String route);

    @GET("FunctionRequest.ashx?action=getSpecialRoute")
    Call<KmbSpecialRouteRes> specialRoute(@Query("route") String route, @Query("bound") String bound);

    @GET("FunctionRequest.ashx?action=getStops")
    Call<KmbStopsRes> stops(@Query("route") String route, @Query("bound") String bound, @Query("serviceType") String serviceType);

    @GET("FunctionRequest.ashx?action=getRoutesInStop")
    Call<KmbRoutesInStop> routesInStop(@Query("bsiCode") String bsiCode);

    Retrofit webSearchCoroutine = new Retrofit.Builder()
            .client(App.Companion.getHttpClient())
            .baseUrl("http://search.kmb.hk/KMBWebSite/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory.create())
            .build();

    @GET("Function/FunctionRequest.ashx?action=getAnnounce")
    Deferred<Response<KmbAnnounceRes>> announce(@Query("route") String route, @Query("bound") String bound);

    @GET("Function/FunctionRequest.ashx?action=getschedule")
    Deferred<Response<KmbScheduleRes>> schedule(@Query("route") String route, @Query("bound") String bound);

    Retrofit webSearchHtmlCoroutine = new Retrofit.Builder()
            .client(App.Companion.getHttpClient())
            .baseUrl("http://search.kmb.hk/KMBWebSite/")
            .addCallAdapterFactory(CoroutineCallAdapterFactory.create())
            .build();

    @GET("AnnouncementPicture.ashx")
    Deferred<Response<ResponseBody>> announcementPicture(@Query("url") String url);

    Retrofit etav3 = new Retrofit.Builder()
            .client(App.Companion.getHttpClient())
            .baseUrl("http://etav3.kmb.hk")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    @GET("?action=geteta")
    Call<KmbEtaRes> eta(@Query("route") String route, @Query("bound") String bound,
                        @Query("stop") String stop, @Query("stop_seq") String stop_seq,
                        @Query("serviceType") String serviceType, @Query("lang") String lang,
                        @Query("updated") String updated);

    Retrofit etav3a = new Retrofit.Builder()
            .client(App.Companion.getHttpClient3())
            .baseUrl("https://etav3.kmb.hk")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    @Headers({
            "Content-Type: application/json",
            "connection: Keep-Alive"
    })
    @POST("?action=geteta")
    Call<KmbEtaRes[]> eta(@Body KmbEtaReq body);

    Gson gsonDate = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(Date.class, new DateDeserializer())
            .create();

    Retrofit etadatafeed = new Retrofit.Builder()
            .client(App.Companion.getHttpClient2())
            .baseUrl("http://etadatafeed.kmb.hk:1933")
            .addConverterFactory(GsonConverterFactory.create(gsonDate))
            .build();

    @GET("GetData.ashx?type=ETA_R")
    Call<List<KmbEtaRoutes>> etaRoutes();

    @GET("GetData.ashx?type=Server_T")
    Call<List<KmbStimeRes>> etaSTime();
}
