package com.clescot.rest;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.json.HealthCheckModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.sql.*;
import java.util.concurrent.TimeUnit;


@Path("/")
public class FooResource {

    private ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final ObjectMapper mapper = new ObjectMapper().registerModules(
            new com.codahale.metrics.json.MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false),//
            new HealthCheckModule()//
    );
    private MetricRegistry registry;
    private DataSource dataSource;

    @Inject
    public FooResource(MetricRegistry registry,DataSource dataSource) {
        this.registry = registry;
        this.dataSource = dataSource;
    }

    @Path("/CPU")
    @GET
    @Gauge(name = "cputime")
    public String getCPU() {
        return "" + threadBean.getCurrentThreadCpuTime();
    }


    @Path("/test")
    @GET
    @Metered
    @Produces(MediaType.TEXT_PLAIN)
    public String testMetered() {

        return "test";
    }


    @Path("/metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String serializeMetricsRegistryInJSON() throws JsonProcessingException {
        return mapper.writeValueAsString(registry);
    }


    @Path("/exception")
    @GET
    @ExceptionMetered
    public String testException() throws JsonProcessingException {
        throw new RuntimeException();
    }


    @Path("/audit")
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public String insert() {
        long id;
        try(Connection connection = dataSource.getConnection()){
            PreparedStatement preparedStatement =connection.prepareStatement("INSERT INTO ACTIVITY ( STARTTIME, ENDTIME, ACTIVITY_NAME) VALUES (?,?,?)");
            DateTime dateTime = new DateTime();
            Date  date = new Date(dateTime.getMillis());
            preparedStatement.setDate(1,date);
            DateTime endDateTime = dateTime.plusDays(3);
            preparedStatement.setDate(1,new Date(dateTime.getMillis()));
            preparedStatement.setDate(2,new Date(endDateTime.getMillis()));
            preparedStatement.setString(3, "foo");
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            if (generatedKeys.next()) {
                 id = generatedKeys.getLong(1);
            } else {
                throw new RuntimeException();
            }

        }catch(SQLException e){
            throw new RuntimeException(e);
        }

        return ""+id;
    }


    @Path("/audit")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String read() {
        StringBuilder stringBuilder = new StringBuilder();
        try(Connection connection = dataSource.getConnection()){
            PreparedStatement preparedStatement =connection.prepareStatement("SELECT id,STARTTIME, ENDTIME, ACTIVITY_NAME FROM ACTIVITY  ");
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
               long id = resultSet.getLong("id");
                stringBuilder.append("id="+id);
               Date startTime = resultSet.getDate("STARTTIME");
                stringBuilder.append(" startTime="+startTime.toString());
                Date endTime = resultSet.getDate("ENDTIME");
                stringBuilder.append(" endTime="+endTime.toString());
                String activity = resultSet.getString("ACTIVITY_NAME");
                stringBuilder.append(" activity="+activity);
                stringBuilder.append("\n");
            }
        }catch(SQLException e){
            throw new RuntimeException(e);
        }

        return stringBuilder.toString();
    }

}
