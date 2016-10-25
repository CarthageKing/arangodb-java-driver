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

import java.util.Collection;

import com.arangodb.entity.LogLevelEntity;
import com.arangodb.entity.UserEntity;
import com.arangodb.internal.ArangoExecutor.ResponseDeserializer;
import com.arangodb.internal.velocystream.Connection;
import com.arangodb.model.DBCreateOptions;
import com.arangodb.model.LogOptions;
import com.arangodb.model.OptionsBuilder;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.velocypack.Type;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.RequestType;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 * @param <R>
 * @param <C>
 *
 */
public class InternalArangoDB<E extends ArangoExecutor<R, C>, R, C extends Connection>
		extends ArangoExecuteable<E, R, C> {

	public InternalArangoDB(final E executor) {
		super(executor);
	}

	protected Request createDatabaseRequest(final String name) {
		final Request request = new Request(ArangoDBConstants.SYSTEM, RequestType.POST,
				ArangoDBConstants.PATH_API_DATABASE);
		request.setBody(executor.serialize(OptionsBuilder.build(new DBCreateOptions(), name)));
		return request;
	}

	protected ResponseDeserializer<Boolean> createDatabaseResponseDeserializer() {
		return new ResponseDeserializer<Boolean>() {
			@Override
			public Boolean deserialize(final Response response) throws VPackException {
				return response.getBody().get(ArangoDBConstants.RESULT).getAsBoolean();
			}
		};
	}

	protected Request getDatabasesRequest(final String database) {
		return new Request(database, RequestType.GET, ArangoDBConstants.PATH_API_DATABASE);
	}

	protected ResponseDeserializer<Collection<String>> getDatabaseResponseDeserializer() {
		return new ResponseDeserializer<Collection<String>>() {
			@Override
			public Collection<String> deserialize(final Response response) throws VPackException {
				final VPackSlice result = response.getBody().get(ArangoDBConstants.RESULT);
				return executor.deserialize(result, new Type<Collection<String>>() {
				}.getType());
			}
		};
	}

	protected Request getAccessibleDatabasesRequest(final String database) {
		return new Request(database, RequestType.GET,
				executor.createPath(ArangoDBConstants.PATH_API_DATABASE, ArangoDBConstants.USER));
	}

	protected Request getVersionRequest() {
		return new Request(ArangoDBConstants.SYSTEM, RequestType.GET, ArangoDBConstants.PATH_API_VERSION);
	}

	protected Request createUserRequest(
		final String database,
		final String user,
		final String passwd,
		final UserCreateOptions options) {
		final Request request;
		request = new Request(database, RequestType.POST, ArangoDBConstants.PATH_API_USER);
		request.setBody(executor
				.serialize(OptionsBuilder.build(options != null ? options : new UserCreateOptions(), user, passwd)));
		return request;
	}

	protected Request deleteUserRequest(final String database, final String user) {
		return new Request(database, RequestType.DELETE, executor.createPath(ArangoDBConstants.PATH_API_USER, user));
	}

	protected Request getUsersRequest(final String database) {
		return new Request(database, RequestType.GET, ArangoDBConstants.PATH_API_USER);
	}

	protected Request getUserRequest(final String database, final String user) {
		return new Request(database, RequestType.GET, executor.createPath(ArangoDBConstants.PATH_API_USER, user));
	}

	protected ResponseDeserializer<Collection<UserEntity>> getUsersResponseDeserializer() {
		return new ResponseDeserializer<Collection<UserEntity>>() {
			@Override
			public Collection<UserEntity> deserialize(final Response response) throws VPackException {
				final VPackSlice result = response.getBody().get(ArangoDBConstants.RESULT);
				return executor.deserialize(result, new Type<Collection<UserEntity>>() {
				}.getType());
			}
		};
	}

	protected Request updateUserRequest(final String database, final String user, final UserUpdateOptions options) {
		final Request request;
		request = new Request(database, RequestType.PATCH, executor.createPath(ArangoDBConstants.PATH_API_USER, user));
		request.setBody(executor.serialize(options != null ? options : new UserUpdateOptions()));
		return request;
	}

	protected Request replaceUserRequest(final String database, final String user, final UserUpdateOptions options) {
		final Request request;
		request = new Request(database, RequestType.PUT, executor.createPath(ArangoDBConstants.PATH_API_USER, user));
		request.setBody(executor.serialize(options != null ? options : new UserUpdateOptions()));
		return request;
	}

	protected Request getLogsRequest(final LogOptions options) {
		final LogOptions params = options != null ? options : new LogOptions();
		return new Request(ArangoDBConstants.SYSTEM, RequestType.GET, ArangoDBConstants.PATH_API_ADMIN_LOG)
				.putQueryParam(LogOptions.PROPERTY_UPTO, params.getUpto())
				.putQueryParam(LogOptions.PROPERTY_LEVEL, params.getLevel())
				.putQueryParam(LogOptions.PROPERTY_START, params.getStart())
				.putQueryParam(LogOptions.PROPERTY_SIZE, params.getSize())
				.putQueryParam(LogOptions.PROPERTY_OFFSET, params.getOffset())
				.putQueryParam(LogOptions.PROPERTY_SEARCH, params.getSearch())
				.putQueryParam(LogOptions.PROPERTY_SORT, params.getSort());
	}

	protected Request getLogLevelRequest() {
		return new Request(ArangoDBConstants.SYSTEM, RequestType.GET, ArangoDBConstants.PATH_API_ADMIN_LOG_LEVEL);
	}

	protected Request setLogLevelRequest(final LogLevelEntity entity) {
		return new Request(ArangoDBConstants.SYSTEM, RequestType.PUT, ArangoDBConstants.PATH_API_ADMIN_LOG_LEVEL)
				.setBody(executor.serialize(entity));
	}
}
