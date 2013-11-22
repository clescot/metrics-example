package com.clescot.rest;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;


public class MetricsModule extends AbstractModule {



    @Override
    protected void configure() {
      HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        bind(HealthCheckRegistry.class).toInstance(healthCheckRegistry);
    }
}
