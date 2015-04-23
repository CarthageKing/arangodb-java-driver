/*
 * Copyright (C) 2012 tamtam180
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

package com.arangodb.impl;

import java.util.Collections;
import java.util.Map;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoException;
import com.arangodb.BaseCursor;
import com.arangodb.InternalCursorDocumentDriver;
import com.arangodb.entity.BaseCursorEntity;
import com.arangodb.entity.DefaultEntity;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.EntityFactory;
import com.arangodb.http.HttpManager;
import com.arangodb.http.HttpResponseEntity;
import com.arangodb.util.MapBuilder;

/**
 * @author tamtam180 - kirscheless at gmail.com
 * @author a-brandt
 *
 */
public class InternalCursorDocumentDriverImpl extends BaseArangoDriverImpl implements InternalCursorDocumentDriver {

	InternalCursorDocumentDriverImpl(ArangoConfigure configure, HttpManager httpManager) {
		super(configure, httpManager);
	}

	@Override
	public BaseCursorEntity<?, ?> validateQuery(String database, String query) throws ArangoException {

		HttpResponseEntity res = httpManager.doPost(createEndpointUrl(database, "/_api/query"), null,
			EntityFactory.toJsonString(new MapBuilder("query", query).get()));

		return createEntity(res, BaseCursorEntity.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, S extends DocumentEntity<T>> BaseCursorEntity<T, S> executeBaseCursorEntityQuery(
		String database,
		String query,
		Map<String, Object> bindVars,
		Class<S> classDocumentEntity,
		Class<T> clazz,
		Boolean calcCount,
		Integer batchSize,
		Boolean fullCount) throws ArangoException {

		HttpResponseEntity res = getCursor(database, query, bindVars, calcCount, batchSize, fullCount);

		try {
			return createEntity(res, BaseCursorEntity.class, classDocumentEntity, clazz);
		} catch (ArangoException e) {
			throw e;
		}

	}

	private HttpResponseEntity getCursor(
		String database,
		String query,
		Map<String, Object> bindVars,
		Boolean calcCount,
		Integer batchSize,
		Boolean fullCount) throws ArangoException {
		HttpResponseEntity res = httpManager.doPost(
			createEndpointUrl(database, "/_api/cursor"),
			null,
			EntityFactory.toJsonString(new MapBuilder().put("query", query)
					.put("bindVars", bindVars == null ? Collections.emptyMap() : bindVars).put("count", calcCount)
					.put("batchSize", batchSize).put("options", new MapBuilder().put("fullCount", fullCount).get())
					.get()));
		return res;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, S extends DocumentEntity<T>> BaseCursorEntity<T, S> continueBaseCursorEntityQuery(
		String database,
		long cursorId,
		Class<S> classDocumentEntity,
		Class<T> clazz) throws ArangoException {

		HttpResponseEntity res = httpManager.doPut(createEndpointUrl(database, "/_api/cursor", cursorId), null, null);

		try {
			return createEntity(res, BaseCursorEntity.class, classDocumentEntity, clazz);
		} catch (ArangoException e) {
			throw e;
		}
	}

	@Override
	public DefaultEntity finishQuery(String database, long cursorId) throws ArangoException {
		HttpResponseEntity res = httpManager.doDelete(createEndpointUrl(database, "/_api/cursor/", cursorId), null);

		try {
			DefaultEntity entity = createEntity(res, DefaultEntity.class);
			return entity;
		} catch (ArangoException e) {
			// TODO Mode
			if (e.getErrorNumber() == 1600) {
				// 既に削除されている
				return (DefaultEntity) e.getEntity();
			}
			throw e;
		}
	}

	@Override
	public <T, S extends DocumentEntity<T>> BaseCursor<T, S> executeBaseCursorQuery(
		String database,
		String query,
		Map<String, Object> bindVars,
		Class<S> classDocumentEntity,
		Class<T> clazz,
		Boolean calcCount,
		Integer batchSize,
		Boolean fullCount) throws ArangoException {

		BaseCursorEntity<T, S> entity = executeBaseCursorEntityQuery(database, query, bindVars, classDocumentEntity,
			clazz, calcCount, batchSize, fullCount);

		return new BaseCursor<T, S>(database, this, entity, classDocumentEntity, clazz);
	}
}
