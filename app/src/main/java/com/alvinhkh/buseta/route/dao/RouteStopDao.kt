package com.alvinhkh.buseta.route.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.alvinhkh.buseta.route.model.RouteStop


@Dao
interface RouteStopDao {

    @Query("DELETE FROM route_stops")
    fun clear(): Int

    @Query("DELETE FROM route_stops WHERE company_code = :companyCode AND last_update < :lastUpdate")
    fun delete(companyCode: String, lastUpdate: Long): Int

    @Query("DELETE FROM route_stops WHERE company_code = :companyCode AND route_no = :routeNo AND last_update < :lastUpdate")
    fun delete(companyCode: String, routeNo: String, lastUpdate: Long): Int

    @Query("DELETE FROM route_stops WHERE company_code = :companyCode AND route_id = :routeId AND route_no = :routeNo AND last_update < :lastUpdate")
    fun delete(companyCode: String, routeId: String, routeNo: String, lastUpdate: Long): Int

    @Query("DELETE FROM route_stops WHERE company_code = :companyCode AND route_id = :routeId AND route_no = :routeNo AND route_sequence = :routeSequence AND route_service_type = :routeServiceType AND last_update < :lastUpdate")
    fun delete(companyCode: String, routeId: String, routeNo: String, routeSequence: String, routeServiceType: String, lastUpdate: Long): Int

    @Query("DELETE FROM route_stops WHERE data_source = :dataSource AND company_code = :companyCode AND last_update < :lastUpdate")
    fun deleteBySource(dataSource: String, companyCode: String, lastUpdate: Long): Int

    @Query("DELETE FROM route_stops WHERE data_source = :dataSource AND company_code = :companyCode AND route_no = :routeNo AND last_update < :lastUpdate")
    fun deleteBySource(dataSource: String, companyCode: String, routeNo: String, lastUpdate: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: RouteStop): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: List<RouteStop>): List<Long>

    @Query("SELECT * FROM route_stops ORDER BY route_sequence + 0 ASC, route_service_type + 0 ASC, sequence + 0 ASC, last_update ASC")
    fun liveData(): LiveData<MutableList<RouteStop>>

    @Query("SELECT * FROM route_stops WHERE company_code = :companyCode AND route_id = :routeId AND route_no = :routeNo AND route_sequence = :routeSequence AND route_service_type = :routeServiceType ORDER BY route_sequence + 0 ASC, route_service_type + 0 ASC, sequence + 0 ASC, last_update ASC")
    fun liveData(companyCode: String, routeId: String, routeNo: String, routeSequence: String, routeServiceType: String): LiveData<MutableList<RouteStop>>

    @Query("SELECT * FROM route_stops WHERE company_code = :companyCode AND route_id = :routeId AND stop_id = :stopId ORDER BY last_update ASC")
    fun get(companyCode: String, routeId: String, stopId: String): MutableList<RouteStop>

    @Query("SELECT * FROM route_stops WHERE company_code = :companyCode AND route_no = :routeNo ORDER BY route_sequence + 0 ASC, route_service_type + 0 ASC, sequence + 0 ASC, last_update ASC")
    fun get(companyCode: String, routeNo: String): MutableList<RouteStop>

    @Query("SELECT * FROM route_stops WHERE data_source = 'DATAGOVHK_NWST' AND company_code = :companyCode AND route_no = :routeNo AND stop_id = :stopId AND route_sequence = :routeSequence AND sequence = :sequence ORDER BY last_update ASC")
    fun getRtNwstStop(companyCode: String, routeNo: String, stopId: String, routeSequence: String, sequence: String): MutableList<RouteStop>

    @Query("SELECT * FROM route_stops WHERE company_code = :companyCode AND route_id = :routeId AND route_no = :routeNo AND route_sequence = :routeSequence AND route_service_type = :routeServiceType ORDER BY route_sequence + 0 ASC, route_service_type + 0 ASC, sequence + 0 ASC, last_update ASC")
    fun get(companyCode: String, routeId: String, routeNo: String, routeSequence: String, routeServiceType: String): MutableList<RouteStop>

    @Query("SELECT * FROM route_stops WHERE company_code = :companyCode AND route_no = :routeNo AND route_sequence = :routeSequence AND route_service_type = :routeServiceType AND stop_id = :stopId AND sequence = :stopsSequence ORDER BY route_sequence + 0 ASC, route_service_type + 0 ASC, sequence + 0 ASC, last_update ASC")
    fun get(companyCode: String, routeNo: String, routeSequence: String, routeServiceType: String, stopId: String, stopsSequence: String): RouteStop?

    @Query("SELECT * FROM route_stops WHERE company_code = :companyCode AND route_id = :routeId AND route_no = :routeNo AND route_sequence = :routeSequence AND route_service_type = :routeServiceType AND stop_id = :stopId AND sequence = :stopsSequence ORDER BY route_sequence + 0 ASC, route_service_type + 0 ASC, sequence + 0 ASC, last_update ASC")
    fun get(companyCode: String, routeId: String, routeNo: String, routeSequence: String, routeServiceType: String, stopId: String, stopsSequence: String): RouteStop?
}