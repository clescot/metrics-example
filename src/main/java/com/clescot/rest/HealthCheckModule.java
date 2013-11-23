package com.clescot.rest;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletModule;
import com.palominolabs.metrics.guice.servlet.AdminServletModule;

public class HealthCheckModule extends ServletModule{

    @Override
    protected void configureServlets() {
        install(new AdminServletModule());
        Multibinder<HealthCheck> healthChecksBinder = Multibinder.newSetBinder(binder(), HealthCheck.class);
        healthChecksBinder.addBinding().to(DatabaseHealthCheck.class);

    }

}
