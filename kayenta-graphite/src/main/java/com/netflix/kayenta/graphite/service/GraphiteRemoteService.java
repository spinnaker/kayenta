package com.netflix.kayenta.graphite.service;

import com.netflix.kayenta.graphite.model.GraphiteResults;
import retrofit.http.GET;
import retrofit.http.Query;

import java.util.List;

public interface GraphiteRemoteService {

    // From https://graphite.readthedocs.io/en/1.1.2/render_api.html#
    @GET("/render")
    List<GraphiteResults> rangeQuery(@Query("target") String target,
                                    @Query("from") int from,
                                    @Query("until") int until,
                                    @Query("format") String format);
}
