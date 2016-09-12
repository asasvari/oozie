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

import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;
import org.apache.oozie.servlet.VersionServlet;
import org.apache.oozie.servlet.V0AdminServlet;
import org.apache.oozie.servlet.V1AdminServlet;
import org.apache.oozie.servlet.V2AdminServlet;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.apache.oozie.servlet.HostnameFilter;

import java.util.EnumSet;

public class EmbeddedOozieServer {
    private Server server;
    private static int OOZIE_HTTP_PORT = 8080;

    private void mapServlets(ServletContextHandler context0) {

        context0.addServlet(new ServletHolder(new VersionServlet()), "/oozie/versions");

        context0.addServlet(new ServletHolder(new V0AdminServlet()), "/oozie/v0/admin/*");
        context0.addServlet(new ServletHolder(new V1AdminServlet()), "/oozie/v1/admin/*");
        context0.addServlet(new ServletHolder(new V2AdminServlet()), "/oozie/v2/admin/*");

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

    private void addFilters(ServletContextHandler context0) {
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


    private void enableSSL(String keystoreFile, String keystorePass,
                          String trustStorePath, String trustStorePass,
                          int httpsPort) {
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        //https.setRequestHeaderSize(10000);
        SslContextFactory sslContextFactory = new SslContextFactory();

        sslContextFactory.setKeyStorePath(keystoreFile);
        sslContextFactory.setKeyManagerPassword(keystorePass);
        sslContextFactory.setTrustStorePath(trustStorePath);
        sslContextFactory.setTrustStorePassword(trustStorePass);
        ServerConnector serverConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https));

        serverConnector.setPort(httpsPort);

        server.setConnectors(new Connector[] { serverConnector });
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void setup() {
        ServletContextHandler context0 = new ServletContextHandler();
        mapServlets(context0);
        addFilters(context0);
        if (isSecured()) {
            //TODO enable SSL
        }
        server.setHandler(context0);
    }

    private void initializeOozieServices() throws ServiceException {
        Services serviceController = new Services();
        serviceController.init();
    }

    private boolean isSecured() {
        return System.getProperty("oozie.https.port") != null;
    }

    public void start() throws Exception {
        initializeOozieServices();
        server.start();
        server.join();
    }

    public static void main(String[] args) throws Exception {
        EmbeddedOozieServer embeddedOozieServer = new EmbeddedOozieServer();
        embeddedOozieServer.setServer(new Server(OOZIE_HTTP_PORT));
        embeddedOozieServer.setup();
        embeddedOozieServer.start();
    }
}