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
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;

import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.NoJspServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.apache.oozie.servlet.HostnameFilter;
import org.eclipse.jetty.webapp.WebAppContext;
import java.util.logging.Logger;

import java.util.List;
import java.util.ArrayList;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.WebAppContext;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.EnumSet;

public class EmbeddedOozieServer {
    private Server server;
    private static int OOZIE_HTTP_PORT = 8080;
    private ServletContextHandler servletContextHandler;

    private void mapServlets() {

        servletContextHandler.addServlet(new ServletHolder(new VersionServlet()), "/oozie/versions");

        servletContextHandler.addServlet(new ServletHolder(new V0AdminServlet()), "/oozie/v0/admin/*");
        servletContextHandler.addServlet(new ServletHolder(new V1AdminServlet()), "/oozie/v1/admin/*");
        servletContextHandler.addServlet(new ServletHolder(new V2AdminServlet()), "/oozie/v2/admin/*");

        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.CallbackServlet()), "/oozie/callback/*");

        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.V0JobsServlet()), "/oozie/v0/jobs");
        //servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.V1JobsServlet()), "/oozie/v1/jobs");

        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.V1JobsServlet()), "/oozie/v2/jobs");

        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.V0JobServlet()), "/oozie/v0/job/*");
        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.V1JobServlet()), "/oozie/v1/job/*");
        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.V2JobServlet()), "/oozie/v2/job/*");

        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.SLAServlet()), "/oozie/v1/sla/*");
        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.V2SLAServlet()), "/oozie/v2/sla/*");

        servletContextHandler.addServlet(new ServletHolder(new org.apache.oozie.servlet.V2ValidateServlet()), "/oozie/v2/validate/*");
    }

    private void addFilters() {
        servletContextHandler.addFilter(new FilterHolder(new HostnameFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));


        FilterHolder authFilter = new FilterHolder(new org.apache.oozie.servlet.AuthFilter());
        servletContextHandler.addFilter(authFilter, "/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "/versions/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "/v0/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "/v1/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "/v2/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "/index.jsp", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "/admin/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "*.js", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "/ext-2.2/*", EnumSet.of(DispatcherType.REQUEST));
        servletContextHandler.addFilter(authFilter, "/docs/*", EnumSet.of(DispatcherType.REQUEST));
    }

    private void setupWebUI() throws URISyntaxException {
        ServletHolder holderDefault = new ServletHolder("default",DefaultServlet.class);
        holderDefault.setInitParameter("resourceBase", this.getClass().getResource("/webapp/").toURI().toASCIIString());
        holderDefault.setInitParameter("dirAllowed","true");
        servletContextHandler.addServlet(holderDefault,"/");

        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        servletContextHandler.setClassLoader(jspClassLoader);
        servletContextHandler.addServlet(jspServletHolder(), "*.jsp");

        Configuration.ClassList classlist = Configuration.ClassList
                .setServerDefault( server );

        classlist.addBefore(
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration" );
        servletContextHandler.setAttribute( "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$" );

        servletContextHandler.setWelcomeFiles(new String[] {"index.jsp"});
        String webDir = EmbeddedOozieServer.class.getClass().getResource("/webapp/").toExternalForm();
        System.out.println("webDir set : " + webDir);
        servletContextHandler.setResourceBase(webDir);
    }

    private ServletHolder jspServletHolder2() throws URISyntaxException {
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("resourceBase", this.getClass().getResource("webapp").toURI().toASCIIString());
        holderJsp.setInitParameter("dirAllowed","true");


        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("keepgenerated", "true");
        File dir = new File("./web/compiled/webapp");
        dir.mkdirs();
        holderJsp.setInitParameter("scratchdir", dir.getAbsolutePath());
        return holderJsp;
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

    public void setup() throws URISyntaxException, IOException {
        servletContextHandler = new ServletContextHandler();
        mapServlets();
        addFilters();
        if (isSecured()) {
            //TODO enable SSL
        }
        //setupWebUI();

        System.setProperty("org.apache.jasper.compiler.disablejsr199","false");

        HandlerCollection myhandlers = new HandlerCollection(true);
        ServletContextHandler context = getWebAppContext(getWebRootResourceUri(), getScratchDir());
        myhandlers.addHandler(context);
        myhandlers.addHandler(servletContextHandler);
        server.setHandler(myhandlers);
        //server.setHandler(servletContextHandler);
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

    /////////////////////////
    private static final String WEBROOT_INDEX = "/webapp/";

    private URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException
    {
        URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
        if (indexUri == null)
        {
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }
        // Points to wherever /webroot/ (the resource) is
        return indexUri.toURI();
    }

    /**
     * Establish Scratch directory for the servlet context (used by JSP compilation)
     */
    private File getScratchDir() throws IOException
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

        if (!scratchDir.exists())
        {
            if (!scratchDir.mkdirs())
            {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        return scratchDir;
    }

    /**
     * Setup the basic application "context" for this application at "/"
     * This is also known as the handler tree (in jetty speak)
     */
    private WebAppContext getWebAppContext(URI baseUri, File scratchDir)
    {
        WebAppContext context = new WebAppContext();
        context.setContextPath("/oozie");
        context.setAttribute("javax.servlet.context.tempdir", scratchDir);
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/.*taglibs.*\\.jar$");
        context.setResourceBase(baseUri.toASCIIString());
        context.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        context.addBean(new ServletContainerInitializersStarter(context), true);
        context.setClassLoader(getUrlClassLoader());

        context.addServlet(jspServletHolder(), "*.jsp");

        context.addServlet(exampleJspFileMappedServletHolder(), "/test/foo/");
        context.addServlet(defaultServletHolder(baseUri), "/");
        return context;
    }

    /**
     * Ensure the jsp engine is initialized correctly
     */
    private List<ContainerInitializer> jspInitializers()
    {
        JettyJasperInitializer sci = new JettyJasperInitializer();
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
        initializers.add(initializer);
        return initializers;
    }

    /**
     * Set Classloader of Context to be sane (needed for JSTL)
     * JSP requires a non-System classloader, this simply wraps the
     * embedded System classloader in a way that makes it suitable
     * for JSP to use
     */
    private ClassLoader getUrlClassLoader()
    {
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        return jspClassLoader;
    }

    /**
     * Create JSP Servlet (must be named "jsp")
     */
    private ServletHolder jspServletHolder()
    {
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.7");
        holderJsp.setInitParameter("compilerSourceVM", "1.7");
        holderJsp.setInitParameter("keepgenerated", "true");
        return holderJsp;
    }

    /**
     * Create Example of mapping jsp to path spec
     */
    private ServletHolder exampleJspFileMappedServletHolder()
    {
        ServletHolder holderAltMapping = new ServletHolder();
        holderAltMapping.setName("index.jsp");
        holderAltMapping.setForcedPath("/index.jsp");
        return holderAltMapping;
    }
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(EmbeddedOozieServer.class.getName());

    /**
     * Create Default Servlet (must be named "default")
     */
    private ServletHolder defaultServletHolder(URI baseUri)
    {
        ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
        LOG.info("Base URI: " + baseUri);
        holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString());
        holderDefault.setInitParameter("dirAllowed", "true");
        return holderDefault;
    }
}