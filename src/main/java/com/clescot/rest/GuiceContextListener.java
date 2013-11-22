package com.clescot.rest;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.palominolabs.metrics.guice.InstrumentationModule;
import com.palominolabs.metrics.guice.servlet.AdminServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class GuiceContextListener extends GuiceServletContextListener {

    public static final String BASE_RESOURCES_PACKAGE = "com.clescot.rest";
    private ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        servletContext = servletContextEvent.getServletContext();
        super.contextInitialized(servletContextEvent);
    }

    @Override
    protected Injector getInjector() {

        Injector injector = Guice.createInjector(new ServletModule() {
                                                     @Override
                                                     protected void configureServlets() {
                                                         install(new AdminServletModule());
                                                         Multibinder<HealthCheck> healthChecksBinder = Multibinder.newSetBinder(binder(), HealthCheck.class);
                                                         healthChecksBinder.addBinding().to(DatabaseHealthCheck.class);

                                                     }

                                                 }, new JerseyServletModule() {

                                                     @Override
                                                     protected void configureServlets() {

                                                         ResourceConfig rc = new PackagesResourceConfig(BASE_RESOURCES_PACKAGE);
                                                         for (Class<?> resource : rc.getClasses()) {
                                                             bind(resource).asEagerSingleton();
                                                         }

                                                         serve("/rest/*").with(GuiceContainer.class);
                                                     }
                                                 }, new InstrumentationModule(),
                new Module() {

                    @Override
                    public void configure(Binder binder) {
                        org.apache.tomcat.jdbc.pool.DataSource h2DataSource = new org.apache.tomcat.jdbc.pool.DataSource();
                        h2DataSource.setUrl("jdbc:h2:mem:test");
                        h2DataSource.setUsername("sa");
                        h2DataSource.setPassword("");
                        h2DataSource.setDriverClassName(org.h2.Driver.class.getName());
                        try(Connection connection = h2DataSource.getConnection();){
                            PreparedStatement preparedStatement = connection.prepareStatement("create table ACTIVITY (ID INTEGER auto_increment,STARTTIME datetime, ENDTIME datetime,  ACTIVITY_NAME VARCHAR(200),PRIMARY KEY (ID) )");
                            preparedStatement.execute();

                        } catch (SQLException e) {
                           throw new RuntimeException(e);
                        }
                        com.soulgalore.jdbcmetrics.DataSource metricsDataSourceProxy = new com.soulgalore.jdbcmetrics.DataSource(h2DataSource);
                        binder.bind(DataSource.class).toInstance(metricsDataSourceProxy);
                    }
                }
        );
        MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        HealthCheckRegistry healthCheckRegistry = injector.getInstance(HealthCheckRegistry.class);
        servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
        servletContext.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
        JmxReporter.forRegistry(metricRegistry).build().start();
        ConsoleReporter.forRegistry(metricRegistry).build().start(10, TimeUnit.SECONDS);
        return injector;
    }
}
