/*
 * $HeadURL$
 * $Revision$
 * $Date$
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.localserver;

import java.net.Socket;

import junit.framework.*;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.conn.Scheme;
import org.apache.http.conn.SchemeRegistry;
import org.apache.http.conn.SocketFactory;
import org.apache.http.conn.PlainSocketFactory;


/**
 * Base class for tests using {@link LocalTestServer LocalTestServer}.
 * Note that the test server will be {@link #setUp set up} before each
 * individual tests and {@link #tearDown teared down} afterwards.
 * Use this base class <i>exclusively</i> for tests that require the
 * server. If you have some tests that require the server and others
 * that don't, split them in two different classes.
 */
public abstract class ServerTestBase extends TestCase {

    /** The local server for testing. */
    protected LocalTestServer localServer;

    /** The available schemes. */
    protected SchemeRegistry supportedSchemes;

    /** The default parameters for the client side. */
    protected HttpParams defaultParams;

    /** The HTTP processor for the client side. */
    protected BasicHttpProcessor httpProcessor;

    /** The default context for the client side. */
    protected HttpExecutionContext httpContext;

    /** The request executor for the client side. */
    protected HttpRequestExecutor httpExecutor;



    protected ServerTestBase(String testName) {
        super(testName);
    }


    /**
     * Prepares the local server for testing.
     * Derived classes that override this method MUST call
     * the implementation here. That SHOULD be done at the
     * beginning of the overriding method.
     * <br/>
     * Derived methods can modify for example the default parameters
     * being set up, or the interceptors.
     * <p>
     * This method will re-use the helper objects from a previous run
     * if they are still available. For example, the local test server
     * will be re-started rather than re-created.
     * Tests that modify the helper objects should afterwards
     * set the respective attributes to <code>null</code> to force
     * re-creation for subsequent tests. Of course that shouldn't
     * be done to the test server, or only after shutting that down.
     *
     * @throws Exception        in case of a problem
     */
    protected void setUp() throws Exception {

        if (defaultParams == null) {
            defaultParams = new BasicHttpParams(null);
            HttpProtocolParams.setVersion
                (defaultParams, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset
                (defaultParams, "UTF-8");
            HttpProtocolParams.setUserAgent
                (defaultParams, "Jakarta-HttpComponents-Test/1.1");
            HttpProtocolParams.setUseExpectContinue
                (defaultParams, false);
        }

        if (supportedSchemes == null) {
            supportedSchemes = new SchemeRegistry();
            SocketFactory sf = PlainSocketFactory.getSocketFactory();
            supportedSchemes.register(new Scheme("http", sf, 80));
        }

        if (httpProcessor == null) {
            httpProcessor = new BasicHttpProcessor();
            httpProcessor.addInterceptor(new RequestContent());
            httpProcessor.addInterceptor(new RequestConnControl()); // optional
        }

        if (httpContext == null) {
            httpContext = new HttpExecutionContext(null);
        }

        if (httpExecutor == null) {
            httpExecutor = new HttpRequestExecutor(httpProcessor);
            httpExecutor.setParams(defaultParams);
        }

        if (localServer == null) {
            localServer = new LocalTestServer(null, null);
            localServer.registerDefaultHandlers();
        }

        localServer.start();

    } // setUp


    /**
     * Unprepares the local server for testing.
     * This stops the test server. All helper objects, including the
     * test server, remain stored in the attributes for the next test.
     *
     * @see #setUp setUp()
     */
    protected void tearDown() throws Exception {
        localServer.stop();
    }


    /**
     * Obtains the address of the local test server.
     *
     * @return  the test server host, with a scheme name of "http"
     */
    protected HttpHost getServerHttp() {

        return new HttpHost(LocalTestServer.TEST_SERVER_ADDR.getHostName(),
                            localServer.getServicePort(),
                            "http");
    }


    /**
     * Opens a connection to the given target using
     * {@link #defaultParams default parameters}.
     * Maps to {@link #connectTo(HttpHost,HttpParams)
     *                 connectTo(target,defaultParams)}.
     *
     * @param target    the target to connect to
     *
     * @return  a new connection opened to the target
     *
     * @throws Exception        in case of a problem
     */
    protected DefaultHttpClientConnection connectTo(HttpHost target)
        throws Exception {

        return connectTo(target, defaultParams);
    }


    /**
     * Opens a connection to the given target using the given parameters.
     *
     * @param target    the target to connect to
     *
     * @return  a new connection opened to the target
     *
     * @throws Exception        in case of a problem
     */
    protected DefaultHttpClientConnection connectTo(HttpHost target,
                                                    HttpParams params)
        throws Exception {

        Scheme schm = supportedSchemes.get(target.getSchemeName());
        int port = schm.resolvePort(target.getPort());

        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        Socket sock = schm.getSocketFactory().connectSocket
            (null, target.getHostName(), target.getPort(), null, 0, params);
        conn.bind(sock, params);

        return conn;
    }


} // class ServerTestBase
