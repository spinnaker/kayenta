package com.netflix.kayenta.wavefront.service;


import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Query;

import java.util.List;
import java.util.Map;

public interface WavefrontRemoteService {
    @GET("/api/v2/chart/api")
    WavefrontTimeSeries fetch(@Header("Authorization") String authorization,
                          @Query("n") String name,
                          @Query("q") String query,
                          @Query("s") Long startTime,
                          @Query("e") Long endTime,
                          @Query("g") String granularity,
                          @Query("summarization") String summarization,
                          @Query("listMode") boolean listMode,
                          @Query("strict") boolean strict,
                          @Query("sorted") boolean sorted);
}
