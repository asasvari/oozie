/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.server;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oozie.service.Services;
import org.apache.oozie.servlet.V1AdminServlet;
import org.apache.oozie.servlet.V2AdminServlet;
import org.apache.oozie.service.JPAService;

import org.apache.oozie.executor.jpa.*;

import javax.persistence.EntityManager;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.sql.*;

import org.apache.oozie.servlet.HostnameFilter;
import org.apache.oozie.servlet.AuthFilter;

import java.util.EnumSet;

public class EmbeddedOozieServer {

    private static void mapServlets(ServletContextHandler context0) {

        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.VersionServlet()), "/oozie/versions");

        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V0AdminServlet()), "/oozie/v0/admin/*");
        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V1AdminServlet()), "/oozie/v1/admin/*");
        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V2AdminServlet()), "/oozie/v2/admin/*");

        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.CallbackServlet()), "/oozie/callback/*");

        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V0JobsServlet()), "/oozie/v0/jobs");
        //context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V1JobsServlet()), "/oozie/v1/jobs");
        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V1JobsServlet()), "/oozie/v2/jobs");

        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V0JobServlet()), "/oozie/v0/job/*");
        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V1JobServlet()), "/oozie/v1/job/*");
        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V2JobServlet()), "/oozie/v2/job/*");

        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.SLAServlet()), "/oozie/v1/sla/*");
        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V2SLAServlet()), "/oozie/v2/sla/*");

        context0.addServlet(new ServletHolder(new org.apache.oozie.servlet.V2ValidateServlet()), "/oozie/v2/validate/*");
    }

    private static void addFilters(ServletContextHandler context0) {
        context0.addFilter(new FilterHolder(new HostnameFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));


        FilterHolder authFilter = new FilterHolder(new org.apache.oozie.servlet.AuthFilter());
        context0.addFilter(authFilter, "/*", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "/versions/*", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "/v0/*", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "/v1/*", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "/v2/*", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "/index.jsp", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "/admin/*", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "*.js", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "/ext-2.2/*", EnumSet.of(DispatcherType.REQUEST));
        context0.addFilter(authFilter, "/docs/*", EnumSet.of(DispatcherType.REQUEST));
    }

    public static void main(String[] args) throws Exception {
        Services serviceController = new Services();
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        Connection conn = DriverManager.getConnection("jdbc:derby:data.db;create=true", "SA", "");
        conn.close();

        serviceController.init();
        JPAService jpaService = Services.get().get(JPAService.class);
        System.out.println(jpaService);
        Server server = new Server(8080);

        /*HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        //https.setRequestHeaderSize(10000);
        SslContextFactory sslContextFactory = new SslContextFactory();

        sslContextFactory.setKeyStorePath("/Users/asasvari/workspace/apache/oozie/jetty/target/oozie-keystore.jks");
        sslContextFactory.setKeyManagerPassword("cloudera");
        sslContextFactory.setTrustStorePath("/Users/asasvari/workspace/apache/oozie/jetty/target/truststore");
        sslContextFactory.setTrustStorePassword("cloudera");
        ServerConnector serverConnector = new ServerConnector(server,
                //new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https));

        serverConnector.setPort(1234);

        server.setConnectors(new Connector[] { serverConnector });
          */
        ServletContextHandler context0 = new ServletContextHandler();
        mapServlets(context0);
        addFilters(context0);
        server.setHandler(context0);

        server.start();
        server.join();
    }
}