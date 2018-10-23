package com.netflix.kayenta.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.netflix.kayenta.graphite.E2EIntegrationTest.CANARY_WINDOW_IN_MINUTES;

@TestConfiguration
@Slf4j
public class GraphiteIntegrationTestConfig {
    public static final String CONTROL_SCOPE_NAME = "control";
    public static final String EXPERIMENT_SCOPE_HEALTHY = "test-healthy";
    public static final String EXPERIMENT_SCOPE_UNHEALTHY = "test-unhealthy";

    private static final String LOCAL_GRAPHITE_HOST = "localhost";
    private static final int LOCAL_GRAPHITE_PORT = 2003;
    private static final String TEST_METRIC = "test.server.request.400";
    private static final int MOCK_SERVICE_REPORTING_INTERVAL_IN_MILLISECONDS = 1000;
    public static final int[] HEALTHY_SERVER_METRICS = {0, 10};
    public static final int[] UNHEALTHY_SERVER_METRICS = {10, 20};

    private final ExecutorService executorService;

    private Instant metricsReportingStartTime;

    public GraphiteIntegrationTestConfig() {
        this.executorService = Executors.newFixedThreadPool(9);
    }

    @Bean
    public Instant metricsReportingStartTime() {
        return metricsReportingStartTime;
    }

    @PostConstruct
    public void start() {
        metricsReportingStartTime = Instant.now();
        executorService.submit(createMetricReportingMockService(CONTROL_SCOPE_NAME, HEALTHY_SERVER_METRICS));
        executorService.submit(createMetricReportingMockService(EXPERIMENT_SCOPE_HEALTHY, HEALTHY_SERVER_METRICS));
        executorService.submit(createMetricReportingMockService(EXPERIMENT_SCOPE_UNHEALTHY, UNHEALTHY_SERVER_METRICS));
        metricsReportingStartTime = Instant.now();
        try {
            long pause = TimeUnit.MINUTES.toMillis(CANARY_WINDOW_IN_MINUTES) + TimeUnit.SECONDS.toMillis(10);
            log.info("Waiting for {} milliseconds for mock data to flow through graphote, before letting the " +
                    "integration" +
                    " tests run", pause);
            Thread.sleep(pause);
        } catch (InterruptedException e) {
            log.error("Failed to wait to send metrics", e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void stop() {
        executorService.shutdownNow();
    }

    public static Runnable createMetricReportingMockService(String scope, int[] countRange) {
        return () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Socket socket = new Socket(LOCAL_GRAPHITE_HOST, LOCAL_GRAPHITE_PORT)) {
                    OutputStream outputStream = socket.getOutputStream();
                    String metrics = TEST_METRIC + "." + scope;
                    String s = String.format("%s %d %d%n", metrics,
                            new Random().nextInt((countRange[1] - countRange[0]) + 1) + countRange[0],
                            Instant.now().getEpochSecond());
                    PrintWriter out = new PrintWriter(outputStream);
                    out.println(s);
                    out.flush();
                    out.close();
                    Thread.sleep(MOCK_SERVICE_REPORTING_INTERVAL_IN_MILLISECONDS);
                } catch (UnknownHostException e) {
                   log.error("UNABLE TO FIND HOST", e);
                } catch (IOException e) {
                    log.error("CONNECTION ERROR", e);
                } catch (InterruptedException e) {
                    log.debug("Thread interrupted", e);
                }
            }
        };
    }
}
