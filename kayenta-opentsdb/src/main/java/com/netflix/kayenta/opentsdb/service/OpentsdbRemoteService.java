package com.netflix.kayenta.opentsdb.service;

import com.netflix.kayenta.opentsdb.model.OpentsdbMetricDescriptorsResponse;
import com.netflix.kayenta.opentsdb.model.OpentsdbResults;
import retrofit.http.GET;
import retrofit.http.Query;

import java.util.List;

public interface OpentsdbRemoteService {

  @GET("/api/query?arrays=true")
  OpentsdbResults fetch(@Query("m") String m,
                        @Query("start") Long start,
                        @Query("end") Long end);

  @GET("/suggest?type=metrics")
  List<String> findMetrics(@Query("q") String q);
}
