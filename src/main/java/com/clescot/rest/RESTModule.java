package com.clescot.rest;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class RESTModule extends JerseyServletModule {
    public static final String BASE_RESOURCES_PACKAGE = "com.clescot.rest";
    @Override
    protected void configureServlets() {

        ResourceConfig rc = new PackagesResourceConfig(BASE_RESOURCES_PACKAGE);
        for (Class<?> resource : rc.getClasses()) {
            bind(resource).asEagerSingleton();
        }

        serve("/rest/*").with(GuiceContainer.class);
    }
}
