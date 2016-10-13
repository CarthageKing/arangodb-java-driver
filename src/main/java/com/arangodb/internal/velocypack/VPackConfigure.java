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

package com.arangodb.internal.velocypack;

import java.lang.reflect.Field;
import java.util.Date;

import org.json.simple.JSONValue;

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.CollectionStatus;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.DocumentField;
import com.arangodb.entity.LogLevel;
import com.arangodb.entity.QueryEntity;
import com.arangodb.internal.CollectionCache;
import com.arangodb.internal.velocystream.AuthenticationRequest;
import com.arangodb.model.TraversalOptions;
import com.arangodb.velocypack.VPack;
import com.arangodb.velocypack.VPackDeserializationContext;
import com.arangodb.velocypack.VPackDeserializer;
import com.arangodb.velocypack.VPackFieldNamingStrategy;
import com.arangodb.velocypack.VPackJsonDeserializer;
import com.arangodb.velocypack.VPackParser;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.ValueType;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocypack.internal.util.NumberUtil;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class VPackConfigure {

	private static final String ID = "_id";

	public static void configure(
		final VPack.Builder builder,
		final VPackParser vpackParser,
		final CollectionCache cache) {

		builder.fieldNamingStrategy(new VPackFieldNamingStrategy() {
			@Override
			public String translateName(final Field field) {
				final DocumentField annotation = field.getAnnotation(DocumentField.class);
				if (annotation != null) {
					return annotation.value().getSerializeName();
				}
				return field.getName();
			}
		});
		builder.registerDeserializer(ID, String.class, new VPackDeserializer<String>() {
			@Override
			public String deserialize(
				final VPackSlice parent,
				final VPackSlice vpack,
				final VPackDeserializationContext context) throws VPackException {
				final String id;
				if (vpack.isCustom()) {
					final long idLong = NumberUtil.toLong(vpack.getBuffer(), vpack.getStart() + 1,
						vpack.getByteSize() - 1);
					final String collectionName = cache.getCollectionName(idLong);
					if (collectionName != null) {
						final VPackSlice key = parent.get("_key");
						id = String.format("%s/%s", collectionName, key.getAsString());
					} else {
						id = null;
					}
				} else {
					id = vpack.getAsString();
				}
				return id;
			}
		});
		vpackParser.registerDeserializer(ID, ValueType.CUSTOM, new VPackJsonDeserializer() {
			@Override
			public void deserialize(
				final VPackSlice parent,
				final String attribute,
				final VPackSlice vpack,
				final StringBuilder json) throws VPackException {
				final String id;
				final long idLong = NumberUtil.toLong(vpack.getBuffer(), vpack.getStart() + 1, vpack.getByteSize() - 1);
				final String collectionName = cache.getCollectionName(idLong);
				if (collectionName != null) {
					final VPackSlice key = parent.get("_key");
					id = String.format("%s/%s", collectionName, key.getAsString());
				} else {
					id = null;
				}
				json.append(JSONValue.toJSONString(id));
			}
		});

		builder.registerSerializer(Request.class, VPackSerializers.REQUEST);
		builder.registerSerializer(AuthenticationRequest.class, VPackSerializers.AUTH_REQUEST);
		builder.registerSerializer(CollectionType.class, VPackSerializers.COLLECTION_TYPE);
		builder.registerSerializer(BaseDocument.class, VPackSerializers.BASE_DOCUMENT);
		builder.registerSerializer(BaseEdgeDocument.class, VPackSerializers.BASE_EDGE_DOCUMENT);
		builder.registerSerializer(TraversalOptions.Order.class, VPackSerializers.TRAVERSAL_ORDER);
		builder.registerSerializer(LogLevel.class, VPackSerializers.LOG_LEVEL);

		builder.registerDeserializer(Response.class, VPackDeserializers.RESPONSE);
		builder.registerDeserializer(CollectionType.class, VPackDeserializers.COLLECTION_TYPE);
		builder.registerDeserializer(CollectionStatus.class, VPackDeserializers.COLLECTION_STATUS);
		builder.registerDeserializer(BaseDocument.class, VPackDeserializers.BASE_DOCUMENT);
		builder.registerDeserializer(BaseEdgeDocument.class, VPackDeserializers.BASE_EDGE_DOCUMENT);
		builder.registerDeserializer(QueryEntity.PROPERTY_STARTED, Date.class, VPackDeserializers.DATE_STRING);
		builder.registerDeserializer(LogLevel.class, VPackDeserializers.LOG_LEVEL);
	}

}
