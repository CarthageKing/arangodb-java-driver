/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.arangodb.ArangoRoute;
import com.arangodb.internal.ArangoExecutor.ResponseDeserializer;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.RequestType;
import com.arangodb.velocystream.Response;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoRouteImpl extends InternalArangoRoute<ArangoDBImpl, ArangoDatabaseImpl, ArangoExecutorSync>
		implements ArangoRoute {

	private final Map<String, String> queryParam;
	private final Map<String, String> headerParam;
	private Object body;

	protected ArangoRouteImpl(final ArangoDatabaseImpl db, final String path, final Map<String, String> headerParam) {
		super(db, path);
		queryParam = new HashMap<String, String>();
		this.headerParam = new HashMap<String, String>();
		this.headerParam.putAll(headerParam);
	}

	@Override
	public ArangoRoute route(final String... path) {
		return new ArangoRouteImpl(db, createPath(this.path, createPath(path)), headerParam);
	}

	@Override
	public ArangoRoute withHeader(final String key, final Object value) {
		if (value != null) {
			headerParam.put(key, value.toString());
		}
		return this;
	}

	@Override
	public ArangoRoute withQueryParam(final String key, final Object value) {
		if (value != null) {
			queryParam.put(key, value.toString());
		}
		return this;
	}

	@Override
	public ArangoRoute withBody(final Object body) {
		this.body = body;
		return this;
	}

	private Response request(final RequestType requestType) {
		final Request request = request(db.name(), requestType, path);
		for (final Entry<String, String> param : headerParam.entrySet()) {
			request.putHeaderParam(param.getKey(), param.getValue());
		}
		for (final Entry<String, String> param : queryParam.entrySet()) {
			request.putQueryParam(param.getKey(), param.getValue());
		}
		if (body != null) {
			request.setBody(util().serialize(body));
		}
		return executor.execute(request, new ResponseDeserializer<Response>() {
			@Override
			public Response deserialize(final Response response) throws VPackException {
				return response;
			}
		});
	}

	@Override
	public Response delete() {
		return request(RequestType.DELETE);
	}

	@Override
	public Response get() {
		return request(RequestType.GET);
	}

	@Override
	public Response head() {
		return request(RequestType.HEAD);
	}

	@Override
	public Response patch() {
		return request(RequestType.PATCH);
	}

	@Override
	public Response post() {
		return request(RequestType.POST);
	}

	@Override
	public Response put() {
		return request(RequestType.PUT);
	}

}
