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

package com.arangodb.internal;

import java.util.HashMap;
import java.util.Map;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.VertexEntity;
import com.arangodb.entity.VertexUpdateEntity;
import com.arangodb.internal.ArangoExecutor.ResponseDeserializer;
import com.arangodb.internal.velocystream.Connection;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.VertexCreateOptions;
import com.arangodb.model.VertexDeleteOptions;
import com.arangodb.model.VertexReplaceOptions;
import com.arangodb.model.VertexUpdateOptions;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.RequestType;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class InternalArangoVertexCollection<E extends ArangoExecutor<R, C>, R, C extends Connection>
		extends ArangoExecuteable<E, R, C> {

	private final String db;
	private final String graph;
	private final String name;

	public InternalArangoVertexCollection(final E executor, final String db, final String graph, final String name) {
		super(executor);
		this.db = db;
		this.graph = graph;
		this.name = name;
	}

	public String name() {
		return name;
	}

	protected String createDocumentHandle(final String key) {
		executor.validateDocumentKey(key);
		return executor.createPath(name, key);
	}

	protected Request dropRequest() {
		return new Request(db, RequestType.DELETE,
				executor.createPath(ArangoDBConstants.PATH_API_GHARIAL, graph, ArangoDBConstants.VERTEX, name));
	}

	protected <T> Request insertVertexRequest(final T value, final VertexCreateOptions options) {
		final Request request = new Request(db, RequestType.POST,
				executor.createPath(ArangoDBConstants.PATH_API_GHARIAL, graph, ArangoDBConstants.VERTEX, name));
		final VertexCreateOptions params = (options != null ? options : new VertexCreateOptions());
		request.putQueryParam(ArangoDBConstants.WAIT_FOR_SYNC, params.getWaitForSync());
		request.setBody(executor.serialize(value));
		return request;
	}

	protected <T> ResponseDeserializer<VertexEntity> insertVertexResponseDeserializer(final T value) {
		return new ResponseDeserializer<VertexEntity>() {
			@Override
			public VertexEntity deserialize(final Response response) throws VPackException {
				final VPackSlice body = response.getBody().get(ArangoDBConstants.VERTEX);
				final VertexEntity doc = executor.deserialize(body, VertexEntity.class);
				final Map<DocumentField.Type, String> values = new HashMap<DocumentField.Type, String>();
				values.put(DocumentField.Type.ID, doc.getId());
				values.put(DocumentField.Type.KEY, doc.getKey());
				values.put(DocumentField.Type.REV, doc.getRev());
				executor.documentCache().setValues(value, values);
				return doc;
			}
		};
	}

	protected Request getVertexRequest(final String key, final DocumentReadOptions options) {
		final Request request = new Request(db, RequestType.GET, executor.createPath(ArangoDBConstants.PATH_API_GHARIAL,
			graph, ArangoDBConstants.VERTEX, createDocumentHandle(key)));
		final DocumentReadOptions params = (options != null ? options : new DocumentReadOptions());
		request.putHeaderParam(ArangoDBConstants.IF_NONE_MATCH, params.getIfNoneMatch());
		request.putHeaderParam(ArangoDBConstants.IF_MATCH, params.getIfMatch());
		return request;
	}

	protected <T> ResponseDeserializer<T> getVertexResponseDeserializer(final Class<T> type) {
		return new ResponseDeserializer<T>() {
			@Override
			public T deserialize(final Response response) throws VPackException {
				return executor.deserialize(response.getBody().get(ArangoDBConstants.VERTEX), type);
			}
		};
	}

	protected <T> Request replaceVertexRequest(final String key, final T value, final VertexReplaceOptions options) {
		final Request request = new Request(db, RequestType.PUT, executor.createPath(ArangoDBConstants.PATH_API_GHARIAL,
			graph, ArangoDBConstants.VERTEX, createDocumentHandle(key)));
		final VertexReplaceOptions params = (options != null ? options : new VertexReplaceOptions());
		request.putQueryParam(ArangoDBConstants.WAIT_FOR_SYNC, params.getWaitForSync());
		request.putHeaderParam(ArangoDBConstants.IF_MATCH, params.getIfMatch());
		request.setBody(executor.serialize(value));
		return request;
	}

	protected <T> ResponseDeserializer<VertexUpdateEntity> replaceVertexResponseDeserializer(final T value) {
		return new ResponseDeserializer<VertexUpdateEntity>() {
			@Override
			public VertexUpdateEntity deserialize(final Response response) throws VPackException {
				final VPackSlice body = response.getBody().get(ArangoDBConstants.VERTEX);
				final VertexUpdateEntity doc = executor.deserialize(body, VertexUpdateEntity.class);
				final Map<DocumentField.Type, String> values = new HashMap<DocumentField.Type, String>();
				values.put(DocumentField.Type.REV, doc.getRev());
				executor.documentCache().setValues(value, values);
				return doc;
			}
		};
	}

	protected <T> Request updateVertexRequest(final String key, final T value, final VertexUpdateOptions options) {
		final Request request;
		request = new Request(db, RequestType.PATCH, executor.createPath(ArangoDBConstants.PATH_API_GHARIAL, graph,
			ArangoDBConstants.VERTEX, createDocumentHandle(key)));
		final VertexUpdateOptions params = (options != null ? options : new VertexUpdateOptions());
		request.putQueryParam(ArangoDBConstants.KEEP_NULL, params.getKeepNull());
		request.putQueryParam(ArangoDBConstants.WAIT_FOR_SYNC, params.getWaitForSync());
		request.putHeaderParam(ArangoDBConstants.IF_MATCH, params.getIfMatch());
		request.setBody(executor.serialize(value, true));
		return request;
	}

	protected <T> ResponseDeserializer<VertexUpdateEntity> updateVertexResponseDeserializer(final T value) {
		return new ResponseDeserializer<VertexUpdateEntity>() {
			@Override
			public VertexUpdateEntity deserialize(final Response response) throws VPackException {
				final VPackSlice body = response.getBody().get(ArangoDBConstants.VERTEX);
				return executor.deserialize(body, VertexUpdateEntity.class);
			}
		};
	}

	protected Request deleteVertexRequest(final String key, final VertexDeleteOptions options) {
		final Request request = new Request(db, RequestType.DELETE, executor.createPath(
			ArangoDBConstants.PATH_API_GHARIAL, graph, ArangoDBConstants.VERTEX, createDocumentHandle(key)));
		final VertexDeleteOptions params = (options != null ? options : new VertexDeleteOptions());
		request.putQueryParam(ArangoDBConstants.WAIT_FOR_SYNC, params.getWaitForSync());
		request.putHeaderParam(ArangoDBConstants.IF_MATCH, params.getIfMatch());
		return request;
	}

}
