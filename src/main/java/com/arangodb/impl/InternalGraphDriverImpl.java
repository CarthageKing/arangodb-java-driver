/*
 * Copyright (C) 2012,2013 tamtam180
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoException;
import com.arangodb.CursorResultSet;
import com.arangodb.InternalCursorDriver;
import com.arangodb.entity.CursorEntity;
import com.arangodb.entity.DeletedEntity;
import com.arangodb.entity.Direction;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.EdgeDefinitionEntity;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.EntityFactory;
import com.arangodb.entity.FilterCondition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.entity.GraphGetCollectionsResultEntity;
import com.arangodb.entity.GraphsEntity;
import com.arangodb.entity.marker.VertexEntity;
import com.arangodb.http.HttpManager;
import com.arangodb.http.HttpResponseEntity;
import com.arangodb.util.MapBuilder;
import com.arangodb.util.StringUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author tamtam180 - kirscheless at gmail.com
 * @since 1.4.0
 */
public class InternalGraphDriverImpl extends BaseArangoDriverWithCursorImpl implements com.arangodb.InternalGraphDriver {

  InternalGraphDriverImpl(ArangoConfigure configure, InternalCursorDriver cursorDriver, HttpManager httpManager) {
    super(configure, cursorDriver, httpManager);
  }

  private String toLower(Enum<?> e) {
    if (e == null) {
      return null;
    }
    return e.name().toLowerCase(Locale.US);
  }

  @Override
  public GraphEntity createGraph(String databaseName, String graphName, Boolean waitForSync) throws ArangoException {
    HttpResponseEntity response = httpManager.doPost(
      createEndpointUrl(baseUrl, databaseName, "/_api/gharial"),
      new MapBuilder().put("waitForSync", waitForSync).get(),
      EntityFactory.toJsonString(new MapBuilder().put("name", graphName).get()));
    return createEntity(response, GraphEntity.class);
  }

  @Override
  public GraphEntity createGraph(
    String databaseName,
    String graphName,
    List<EdgeDefinitionEntity> edgeDefinitions,
    List<String> orphanCollections,
    Boolean waitForSync) throws ArangoException {

    HttpResponseEntity response = httpManager.doPost(
      createEndpointUrl(baseUrl, databaseName, "/_api/gharial"),
      new MapBuilder().put("waitForSync", waitForSync).get(),
      EntityFactory.toJsonString(new MapBuilder().put("name", graphName).put("edgeDefinitions", edgeDefinitions)
          .put("orphanCollections", orphanCollections).get()));
    return createEntity(response, GraphEntity.class);
  }

  @Override
  public GraphsEntity getGraphs(String databaseName) throws ArangoException {

    GraphsEntity graphsEntity = new GraphsEntity();
    List<GraphEntity> graphEntities = new ArrayList<GraphEntity>();
    List<String> graphList = this.getGraphList(databaseName);
    if (CollectionUtils.isNotEmpty(graphList)) {
      for (String graphName : graphList) {
        graphEntities.add(this.getGraph(databaseName, graphName));
      }
    }
    graphsEntity.setGraphs(graphEntities);
    return graphsEntity;

  }

  @Override
  public List<String> getGraphList(String databaseName) throws ArangoException {
    HttpResponseEntity res = httpManager.doGet(createEndpointUrl(baseUrl, databaseName, "/_api/gharial"));
    GraphsEntity graphsEntity = createEntity(res, GraphsEntity.class);
    List<String> graphList = new ArrayList<String>();
    List<GraphEntity> graphs = graphsEntity.getGraphs();
    if (CollectionUtils.isNotEmpty(graphs)) {
      for (GraphEntity graph : graphs) {
        graphList.add(graph.getDocumentKey());
      }
    }
    return graphList;
  }

  @Override
  public GraphEntity getGraph(String databaseName, String graphName) throws ArangoException {
    validateCollectionName(graphName); // ??
    HttpResponseEntity res = httpManager.doGet(
      createEndpointUrl(baseUrl, databaseName, "/_api/gharial", StringUtils.encodeUrl(graphName)),
      new MapBuilder().get(),
      null);
    GraphEntity g = createEntity(res, GraphEntity.class);
    return createEntity(res, GraphEntity.class);

  }

  @Override
  public DeletedEntity deleteGraph(String databaseName, String graphName, Boolean dropCollections)
      throws ArangoException {
    validateCollectionName(graphName); // ??
    HttpResponseEntity res = httpManager.doDelete(
      createEndpointUrl(baseUrl, databaseName, "/_api/gharial", StringUtils.encodeUrl(graphName)),
      new MapBuilder().get(),
      new MapBuilder().put("dropCollections", dropCollections).get());

    if (!res.isJsonResponse()) {
      throw new ArangoException("unknown error");
    }

    DeletedEntity result = createEntity(res, DeletedEntity.class, null, true);

    return result;

  }

  @Override
  public List<String> getVertexCollections(String databaseName, String graphName) throws ArangoException {
    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doGet(createEndpointUrl(
      baseUrl,
      databaseName,
      "/_api/gharial",
      StringUtils.encodeUrl(graphName),
      "/vertex"));

    if (!res.isJsonResponse()) {
      throw new ArangoException("unknown error");
    }

    GraphGetCollectionsResultEntity result = createEntity(res, GraphGetCollectionsResultEntity.class, null, true);

    return result.getCollections();
  }

  /**
   * Removes a vertex collection from the graph and optionally deletes the
   * collection, if it is not used in any other graph.
   *
   * @param databaseName
   * @param graphName
   * @param collectionName
   * @param dropCollection
   * @throws ArangoException
   */
  @Override
  public DeletedEntity deleteVertexCollection(
    String databaseName,
    String graphName,
    String collectionName,
    Boolean dropCollection) throws ArangoException {
    validateDatabaseName(databaseName, false);
    validateCollectionName(collectionName);
    validateCollectionName(graphName);

    HttpResponseEntity res = httpManager.doDelete(
      createEndpointUrl(
        baseUrl,
        databaseName,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "/vertex",
        StringUtils.encodeUrl(collectionName)),
      new MapBuilder().get(),
      new MapBuilder().put("dropCollection", dropCollection).get());

    DeletedEntity result = createEntity(res, DeletedEntity.class, null, true);

    return result;

  }

  @Override
  public GraphEntity createVertexCollection(String databaseName, String graphName, String collectionName)
      throws ArangoException {
    validateCollectionName(graphName);
    validateCollectionName(collectionName);

    HttpResponseEntity res = httpManager.doPost(
      createEndpointUrl(baseUrl, databaseName, "/_api/gharial", StringUtils.encodeUrl(graphName), "/vertex"),
      null,
      EntityFactory.toJsonString(new MapBuilder().put("collection", collectionName).get()));

    if (!res.isJsonResponse()) {
      throw new ArangoException("unknown error");
    }

    GraphEntity result = createEntity(res, GraphEntity.class, null, true);

    return result;
  }

  @Override
  public List<String> getEdgeCollections(String databaseName, String graphName) throws ArangoException {
    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doGet(createEndpointUrl(
      baseUrl,
      databaseName,
      "/_api/gharial",
      StringUtils.encodeUrl(graphName),
      "/edge"));

    if (!res.isJsonResponse()) {
      throw new ArangoException("unknown error");
    }

    GraphGetCollectionsResultEntity result = createEntity(res, GraphGetCollectionsResultEntity.class, null, true);

    return result.getCollections();
  }

  @Override
  public GraphEntity createEdgeDefinition(String databaseName, String graphName, EdgeDefinitionEntity edgeDefinition)
      throws ArangoException {

    validateCollectionName(graphName);
    validateCollectionName(edgeDefinition.getCollection());

    String edgeDefinitionJson = this.convertToString(edgeDefinition);

    HttpResponseEntity res = httpManager.doPost(
      createEndpointUrl(baseUrl, databaseName, "/_api/gharial", StringUtils.encodeUrl(graphName), "/edge"),
      null,
      edgeDefinitionJson);

    if (!res.isJsonResponse()) {
      throw new ArangoException("unknown error");
    }

    GraphEntity result = createEntity(res, GraphEntity.class, null, true);

    return result;
  }

  @Override
  public GraphEntity replaceEdgeDefinition(
    String databaseName,
    String graphName,
    String edgeName,
    EdgeDefinitionEntity edgeDefinition) throws ArangoException {

    validateCollectionName(graphName);
    validateCollectionName(edgeDefinition.getCollection());

    String edgeDefinitionJson = this.convertToString(edgeDefinition);

    HttpResponseEntity res = httpManager.doPut(
      createEndpointUrl(
        baseUrl,
        databaseName,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "/edge",
        StringUtils.encodeUrl(edgeName)),
      null,
      edgeDefinitionJson);
    if (!res.isJsonResponse()) {
      throw new ArangoException("unknown error");
    }

    GraphEntity result = createEntity(res, GraphEntity.class, null, true);

    return result;

  }

  @Override
  public GraphEntity
      deleteEdgeDefinition(String databaseName, String graphName, String edgeName, Boolean dropCollection)
          throws ArangoException {
    validateCollectionName(graphName);
    validateCollectionName(edgeName);

    HttpResponseEntity res = httpManager.doDelete(
      createEndpointUrl(
        baseUrl,
        databaseName,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "/edge",
        StringUtils.encodeUrl(edgeName)),
      new MapBuilder().put("dropCollection", dropCollection).get());
    if (!res.isJsonResponse()) {
      throw new ArangoException("unknown error");
    }

    GraphEntity result = createEntity(res, GraphEntity.class, null, true);

    return result;
  }

  @Override
  public <T> DocumentEntity<T> createVertex(
    String database,
    String graphName,
    String collectionName,
    Object vertex,
    Boolean waitForSync) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doPost(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "vertex",
        StringUtils.encodeUrl(collectionName)),
      new MapBuilder().put("waitForSync", waitForSync).get(),
      EntityFactory.toJsonString(vertex));

    if (!res.isJsonResponse()) {
      throw new ArangoException("unknown error");
    }
    return createEntity(res, VertexEntity.class, vertex.getClass());
  }

  @Override
  public <T> DocumentEntity<T> getVertex(
    String databaseName,
    String graphName,
    String collectionName,
    String key,
    Class<?> clazz,
    Long rev,
    Long ifNoneMatchRevision,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doGet(
      createEndpointUrl(
        baseUrl,
        StringUtils.encodeUrl(databaseName),
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "vertex",
        StringUtils.encodeUrl(collectionName),
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-None-Match", ifNoneMatchRevision, true).put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("rev", rev).get());

    return createEntity(res, VertexEntity.class, clazz);

  }

  @Override
  public <T> DocumentEntity<T> replaceVertex(
    String databaseName,
    String graphName,
    String collectionName,
    String key,
    Object vertex,
    Boolean waitForSync,
    Long rev,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doPut(
      createEndpointUrl(
        baseUrl,
        StringUtils.encodeUrl(databaseName),
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "vertex",
        StringUtils.encodeUrl(collectionName),
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("waitForSync", waitForSync).put("rev", rev).get(),
      EntityFactory.toJsonString(vertex));

    return createEntity(res, VertexEntity.class, vertex.getClass());

  }

  @Override
  public <T> DocumentEntity<T> updateVertex(
    String databaseName,
    String graphName,
    String collectionName,
    String key,
    Object vertex,
    Boolean keepNull,
    Boolean waitForSync,
    Long rev,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doPatch(
      createEndpointUrl(
        baseUrl,
        databaseName,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "vertex",
        StringUtils.encodeUrl(collectionName),
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("keepNull", keepNull).put("waitForSync", waitForSync).put("rev", rev).get(),
      EntityFactory.toJsonString(vertex, keepNull != null && !keepNull));

    return createEntity(res, VertexEntity.class, vertex.getClass());
  }

  @Override
  public DeletedEntity deleteVertex(
    String databaseName,
    String graphName,
    String collectionName,
    String key,
    Boolean waitForSync,
    Long rev,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doDelete(
      createEndpointUrl(
        baseUrl,
        StringUtils.encodeUrl(databaseName),
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "vertex",
        StringUtils.encodeUrl(collectionName),
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("waitForSync", waitForSync).put("rev", rev).get());

    return createEntity(res, DeletedEntity.class);
  }

  @Override
  public <T> EdgeEntity<T> createEdge(
    String database,
    String graphName,
    String edgeCollectionName,
    String key,
    String fromHandle,
    String toHandle,
    Object value,
    Boolean waitForSync) throws ArangoException {

    JsonObject obj;
    if (value == null) {
      obj = new JsonObject();
    } else {
      JsonElement elem = EntityFactory.toJsonElement(value, false);
      if (elem.isJsonObject()) {
        obj = elem.getAsJsonObject();
      } else {
        throw new IllegalArgumentException("value need object type(not support array, primitive, etc..).");
      }
    }
    obj.addProperty("_key", key);
    obj.addProperty("_from", fromHandle);
    obj.addProperty("_to", toHandle);

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doPost(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "/edge",
        StringUtils.encodeUrl(edgeCollectionName)),
      new MapBuilder().put("waitForSync", waitForSync).get(),
      EntityFactory.toJsonString(obj));

    EdgeEntity<T> entity = createEntity(res, EdgeEntity.class, value == null ? null : value.getClass());

    entity.setFromVertexHandle(fromHandle);
    entity.setToVertexHandle(toHandle);
    return entity;

  }

  @Override
  public <T> EdgeEntity<T> getEdge(
    String database,
    String graphName,
    String edgeCollectionName,
    String key,
    Class<?> clazz,
    Long rev,
    Long ifNoneMatchRevision,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doGet(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "edge",
        StringUtils.encodeUrl(edgeCollectionName),
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-None-Match", ifNoneMatchRevision, true).put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("rev", rev).get());

    return createEntity(res, EdgeEntity.class, clazz);

  }

  @Override
  public DeletedEntity deleteEdge(
    String database,
    String graphName,
    String edgeCollectionName,
    String key,
    Boolean waitForSync,
    Long rev,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doDelete(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "edge",
        StringUtils.encodeUrl(edgeCollectionName),
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("waitForSync", waitForSync).put("rev", rev).get());

    return createEntity(res, DeletedEntity.class);

  }

  @Override
  public <T> EdgeEntity<T> replaceEdge(
    String database,
    String graphName,
    String edgeCollectionName,
    String key,
    Object value,
    Boolean waitForSync,
    Long rev,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doPut(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/gharial",
        StringUtils.encodeUrl(graphName),
        "/edge",
        StringUtils.encodeUrl(edgeCollectionName),
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("waitForSync", waitForSync).put("rev", rev).get(),
      value == null ? null : EntityFactory.toJsonString(value));

    return createEntity(res, EdgeEntity.class, value == null ? null : value.getClass());

  }

  // ****************************************************************************

  @Override
  public <T> CursorEntity<DocumentEntity<T>> getVertices(
    String database,
    String graphName,
    String vertexKey,
    Class<?> clazz,
    Integer batchSize,
    Integer limit,
    Boolean count,
    Direction direction,
    Collection<String> labels,
    FilterCondition... properties) throws ArangoException {

    validateCollectionName(graphName);
    Map<String, Object> filter = new MapBuilder().put("direction", toLower(direction)).put("labels", labels)
        .put("properties", properties).get();

    HttpResponseEntity res = httpManager.doPost(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/graph",
        StringUtils.encodeUrl(graphName),
        "vertices",
        StringUtils.encodeUrl(vertexKey)),
      null,
      EntityFactory.toJsonString(new MapBuilder().put("batchSize", batchSize).put("limit", limit).put("count", count)
          .put("filter", filter).get()));

    return createEntity(res, CursorEntity.class, DocumentEntity.class, clazz);

  }

  @Override
  public <T> CursorResultSet<DocumentEntity<T>> getVerticesWithResultSet(
    String database,
    String graphName,
    String vertexKey,
    Class<?> clazz,
    Integer batchSize,
    Integer limit,
    Boolean count,
    Direction direction,
    Collection<String> labels,
    FilterCondition... properties) throws ArangoException {

    CursorEntity<DocumentEntity<T>> entity = getVertices(
      database,
      graphName,
      vertexKey,
      clazz,
      batchSize,
      limit,
      count,
      direction,
      labels,
      properties);
    CursorResultSet<DocumentEntity<T>> rs = new CursorResultSet<DocumentEntity<T>>(database, cursorDriver, entity,
        DocumentEntity.class, clazz);
    return rs;
  }

  // @Override
  // public <T> EdgeEntity<T> createEdge(
  // String database,
  // String graphName,
  // String key,
  // String fromHandle,
  // String toHandle,
  // Object value,
  // String label,
  // Boolean waitForSync) throws ArangoException {
  //
  // JsonObject obj;
  // if (value == null) {
  // obj = new JsonObject();
  // } else {
  // JsonElement elem = EntityFactory.toJsonElement(value, false);
  // if (elem.isJsonObject()) {
  // obj = elem.getAsJsonObject();
  // } else {
  // throw new
  // IllegalArgumentException("value need object type(not support array, primitive, etc..).");
  // }
  // }
  // obj.addProperty("_key", key);
  // obj.addProperty("_from", fromHandle);
  // obj.addProperty("_to", toHandle);
  // obj.addProperty("$label", label);
  //
  // validateCollectionName(graphName);
  // HttpResponseEntity res = httpManager.doPost(
  // createEndpointUrl(baseUrl, database, "/_api/graph",
  // StringUtils.encodeUrl(graphName), "/edge"),
  // new MapBuilder().put("waitForSync", waitForSync).get(),
  // EntityFactory.toJsonString(obj));
  //
  // return createEntity(res, EdgeEntity.class, value == null ? null :
  // value.getClass());
  //
  // }

  @Override
  public <T> EdgeEntity<T> getEdge(
    String database,
    String graphName,
    String key,
    Class<?> clazz,
    Long rev,
    Long ifNoneMatchRevision,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doGet(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/graph",
        StringUtils.encodeUrl(graphName),
        "edge",
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-None-Match", ifNoneMatchRevision, true).put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("rev", rev).get());

    return createEntity(res, EdgeEntity.class, clazz);

  }

  @Override
  public DeletedEntity deleteEdge(
    String database,
    String graphName,
    String key,
    Boolean waitForSync,
    Long rev,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doDelete(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/graph",
        StringUtils.encodeUrl(graphName),
        "edge",
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("waitForSync", waitForSync).put("rev", rev).get());

    return createEntity(res, DeletedEntity.class);

  }

  @Override
  public <T> EdgeEntity<T> replaceEdge(
    String database,
    String graphName,
    String key,
    Object value,
    Boolean waitForSync,
    Long rev,
    Long ifMatchRevision) throws ArangoException {

    validateCollectionName(graphName);
    HttpResponseEntity res = httpManager.doPut(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/graph",
        StringUtils.encodeUrl(graphName),
        "/edge",
        StringUtils.encodeUrl(key)),
      new MapBuilder().put("If-Match", ifMatchRevision, true).get(),
      new MapBuilder().put("waitForSync", waitForSync).put("rev", rev).get(),
      value == null ? null : EntityFactory.toJsonString(value));

    return createEntity(res, EdgeEntity.class, value == null ? null : value.getClass());

  }

  @Override
  public <T> CursorEntity<EdgeEntity<T>> getEdges(
    String database,
    String graphName,
    String vertexKey,
    Class<?> clazz,
    Integer batchSize,
    Integer limit,
    Boolean count,
    Direction direction,
    Collection<String> labels,
    FilterCondition... properties) throws ArangoException {

    validateCollectionName(graphName);

    Map<String, Object> filter = new MapBuilder().put("direction", toLower(direction)).put("labels", labels)
        .put("properties", properties).get();

    HttpResponseEntity res = httpManager.doPost(
      createEndpointUrl(
        baseUrl,
        database,
        "/_api/graph",
        StringUtils.encodeUrl(graphName),
        "edges",
        StringUtils.encodeUrl(vertexKey)),
      null,
      EntityFactory.toJsonString(new MapBuilder().put("batchSize", batchSize).put("limit", limit).put("count", count)
          .put("filter", filter).get()));

    return createEntity(res, CursorEntity.class, EdgeEntity.class, clazz);

  }

  @Override
  public <T> CursorResultSet<EdgeEntity<T>> getEdgesWithResultSet(
    String database,
    String graphName,
    String vertexKey,
    Class<?> clazz,
    Integer batchSize,
    Integer limit,
    Boolean count,
    Direction direction,
    Collection<String> labels,
    FilterCondition... properties) throws ArangoException {

    CursorEntity<EdgeEntity<T>> entity = getEdges(
      database,
      graphName,
      vertexKey,
      clazz,
      batchSize,
      limit,
      count,
      direction,
      labels,
      properties);
    CursorResultSet<EdgeEntity<T>> rs = new CursorResultSet<EdgeEntity<T>>(database, cursorDriver, entity,
        EdgeEntity.class, clazz);
    return rs;
  }

  private String convertToString(EdgeDefinitionEntity edgeDefinition) {
    JsonObject rawEdgeDefinition = (JsonObject) EntityFactory.toJsonElement(
      new MapBuilder().put("edgeDefinition", edgeDefinition).get(),
      false);
    JsonElement edgeDefinitionJson = rawEdgeDefinition.get("edgeDefinition");
    return edgeDefinitionJson.toString();
  }

}
