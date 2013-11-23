package com.clescot.rest;

import com.google.inject.Binder;
import com.google.inject.Module;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JDBCMetricsModule implements Module {

    public static final String JDBC_H2_URL = "jdbc:h2:mem:test";
    public static final String USERNAME = "sa";
    public static final String PASSWORD = "";
    public static final String CREATE_TABLE_FOR_AUDIT = "create table ACTIVITY (ID INTEGER auto_increment,STARTTIME datetime, ENDTIME datetime,  ACTIVITY_NAME VARCHAR(200),PRIMARY KEY (ID) )";

    @Override
    public void configure(Binder binder) {
        org.apache.tomcat.jdbc.pool.DataSource h2DataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        h2DataSource.setUrl(JDBC_H2_URL);
        h2DataSource.setUsername(USERNAME);
        h2DataSource.setPassword(PASSWORD);
        h2DataSource.setDriverClassName(org.h2.Driver.class.getName());
        try(Connection connection = h2DataSource.getConnection()){
            PreparedStatement preparedStatement = connection.prepareStatement(CREATE_TABLE_FOR_AUDIT);
            preparedStatement.execute();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        com.soulgalore.jdbcmetrics.DataSource metricsDataSourceProxy = new com.soulgalore.jdbcmetrics.DataSource(h2DataSource);
        binder.bind(DataSource.class).toInstance(metricsDataSourceProxy);
    }
}
