package com.netflix.kayenta.graphite.service;

import com.netflix.kayenta.graphite.model.GraphiteMetricDescriptorsResponse;
import com.netflix.kayenta.graphite.model.GraphiteResults;
import retrofit.http.GET;
import retrofit.http.Query;

import java.util.List;

public interface GraphiteRemoteService {

    // From https://graphite.readthedocs.io/en/1.1.2/render_api.html#
    @GET("/render")
    List<GraphiteResults> rangeQuery(@Query(value = "target", encodeValue = false) String target,
                                    @Query("from") long from,
                                    @Query("until") long until,
                                    @Query("format") String format);

    @GET("/metrics/find")
    GraphiteMetricDescriptorsResponse findMetrics(@Query("query") String query, @Query("format") String format);
}
