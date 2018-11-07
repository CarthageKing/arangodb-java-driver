/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.internal.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.arangodb.internal.util.IOUtils;

/**
 * @author Mark Vollmary
 *
 */
public class ConnectionPoolImpl implements ConnectionPool {

	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ConnectionPoolImpl.class);

	private final HostDescription host;
	private final int maxConnections;
	private final List<Connection> connections;
	private int current;
	private final ConnectionFactory factory;

	public ConnectionPoolImpl(final HostDescription host, final Integer maxConnections,
		final ConnectionFactory factory) {
		super();
		this.host = host;
		this.maxConnections = maxConnections;
		this.factory = factory;
		connections = new ArrayList<Connection>();
		current = 0;

		if (LOGGER.isWarnEnabled()) {
			// instantiate an exception so we know the stacktrace to know the instances when this is invoked
			Exception ex = new RuntimeException();
			LOGGER.warn("Instantiated a connection pool {}\n", this, ex);
		}
	}

	@Override
	public Connection createConnection(final HostDescription host) {
		return factory.create(host);
	}

	@Override
	public synchronized Connection connection() {
		final Connection connection;
		if (connections.size() < maxConnections) {
			connection = createConnection(host);
			connections.add(connection);
			current++;
		} else {
			final int index = (current++) % connections.size();
			connection = connections.get(index);
		}
		return connection;
	}

	@Override
	public void close() throws IOException {
		for (final Connection connection : connections) {
			IOUtils.closeQuietly(connection);
		}
		connections.clear();
		LOGGER.warn("connection pool {} is closing", this);
	}

}
