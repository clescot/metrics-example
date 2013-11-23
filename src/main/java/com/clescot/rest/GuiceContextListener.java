package com.clescot.rest;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.palominolabs.metrics.guice.InstrumentationModule;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.concurrent.TimeUnit;

public class GuiceContextListener extends GuiceServletContextListener {


    private ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        servletContext = servletContextEvent.getServletContext();
        super.contextInitialized(servletContextEvent);
    }

    @Override
    protected Injector getInjector() {

        Injector injector = Guice.createInjector(new HealthCheckModule(), new RESTModule(), new InstrumentationModule(),
                new JDBCMetricsModule()
        );

        //register registries in ServletContext
        MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        HealthCheckRegistry healthCheckRegistry = injector.getInstance(HealthCheckRegistry.class);
        servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
        servletContext.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);

        //configure reporters
        JmxReporter.forRegistry(metricRegistry).build().start();
        ConsoleReporter.forRegistry(metricRegistry).build().start(10, TimeUnit.SECONDS);
        return injector;
    }
}
