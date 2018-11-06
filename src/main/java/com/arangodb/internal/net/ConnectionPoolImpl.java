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

/**
 * @author Mark Vollmary
 *
 */
public class ConnectionPoolImpl implements ConnectionPool {

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
			if (current < 0) {
				current = 0;
			}
			final int index = current % connections.size();
			current++;
			connection = connections.get(index);
		}
		return connection;
	}

	@Override
	public void close() throws IOException {
		for (final Connection connection : connections) {
			connection.close();
		}
		connections.clear();
	}

}
