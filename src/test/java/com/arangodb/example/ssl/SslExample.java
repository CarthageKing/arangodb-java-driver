/*
 * Copyright (C) 2015 ArangoDB GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arangodb.example.ssl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContexts;
import org.junit.Assert;
import org.junit.Test;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.ArangoHost;
import com.arangodb.entity.ArangoVersion;
import com.arangodb.http.HttpResponseEntity;

/*-
 * Example for using a HTTPS connection
 * 
 * Create a self signed certificate for arangod (or use the test certificate of the unit tests)
 * https://docs.arangodb.com/ConfigureArango/Arangod.html
 * 
 * Start arangod with HTTP (port 8529) and HTTPS (port 8530): 
 * 
 * bin/arangod 
 *            --server.disable-authentication=false 
 *            --configuration ./etc/relative/arangod.conf 
 *            --server.endpoint ssl://localhost:8530 
 *            --server.keyfile UnitTests/server.pem 
 *            --server.endpoint tcp://localhost:8529
 *            ../database/ 
 * 
 * @author a-brandt
 *
 */
public class SslExample {

	private static final String SSL_TRUSTSTORE_PASSWORD = "12345678";

	/*-
	 * a SSL trust store
	 * 
	 * create the trust store for the self signed certificate:
	 * keytool -import -alias "my arangodb server cert" -file UnitTests/server.pem -keystore example.truststore
	 * 
	 * Documentation:
	 * https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/conn/ssl/SSLSocketFactory.html
	 */
	private static final String SSL_TRUSTSTORE = "/example.truststore";

	@Test
	public void httpTest() throws ArangoException {

		ArangoConfigure configuration = new ArangoConfigure();
		// get host and port from arangodb.properties
		// configuration.setArangoHost(new ArangoHost("localhost", 8529));
		configuration.init();

		ArangoDriver arangoDriver = new ArangoDriver(configuration);

		ArangoVersion version = arangoDriver.getVersion();
		Assert.assertNotNull(version);

	}

	@Test
	public void sslConnectionTest() throws ArangoException {
		// use HTTPS with java default trust store

		ArangoConfigure configuration = new ArangoConfigure();
		configuration.setArangoHost(new ArangoHost("www.arangodb.com", 443));
		configuration.setUseSsl(true);
		configuration.init();

		ArangoDriver arangoDriver = new ArangoDriver(configuration);

		HttpResponseEntity response = arangoDriver.getHttpManager().doGet("/");
		Assert.assertEquals(200, response.getStatusCode());
	}

	@Test
	public void sslWithSelfSignedCertificateTest() throws ArangoException, KeyManagementException,
			NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, URISyntaxException {

		// create a sslContext for the self signed certificate
		URL resource = this.getClass().getResource(SSL_TRUSTSTORE);
		SSLContext sslContext = SSLContexts.custom()
				.loadTrustMaterial(Paths.get(resource.toURI()).toFile(), SSL_TRUSTSTORE_PASSWORD.toCharArray()).build();

		ArangoConfigure configuration = new ArangoConfigure("/ssl-arangodb.properties");
		configuration.setSslContext(sslContext);
		configuration.init();

		ArangoDriver arangoDriver = new ArangoDriver(configuration);

		ArangoVersion version = arangoDriver.getVersion();
		Assert.assertNotNull(version);
	}

	@Test
	public void sslHandshakeExceptionTest() {
		ArangoConfigure configuration = new ArangoConfigure("/ssl-arangodb.properties");
		configuration.init();

		ArangoDriver arangoDriver = new ArangoDriver(configuration);

		try {
			// java do not trust self signed certificates

			arangoDriver.getVersion();
			Assert.fail("this should fail");

		} catch (ArangoException e) {
			Throwable cause = e.getCause();
			Assert.assertTrue(cause instanceof javax.net.ssl.SSLHandshakeException);
		}
	}

	@Test
	public void sslPeerUnverifiedExceptionTest() throws ArangoException, KeyManagementException,
			NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, URISyntaxException {

		// create a sslContext for the self signed certificate
		URL resource = this.getClass().getResource(SSL_TRUSTSTORE);
		SSLContext sslContext = SSLContexts.custom()
				.loadTrustMaterial(Paths.get(resource.toURI()).toFile(), SSL_TRUSTSTORE_PASSWORD.toCharArray()).build();

		ArangoConfigure configuration = new ArangoConfigure("/ssl-arangodb.properties");
		// 127.0.0.1 is the wrong name
		configuration.getArangoHost().setHost("127.0.0.1");
		configuration.setSslContext(sslContext);
		configuration.init();

		ArangoDriver arangoDriver = new ArangoDriver(configuration);

		try {
			arangoDriver.getVersion();
			Assert.fail("this should fail");
		} catch (ArangoException e) {
			Throwable cause = e.getCause();
			Assert.assertTrue(cause instanceof javax.net.ssl.SSLPeerUnverifiedException);
		}

	}

}
