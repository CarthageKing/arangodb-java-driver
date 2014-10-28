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

import java.util.TreeMap;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoException;
import com.arangodb.entity.BooleanResultEntity;
import com.arangodb.entity.DatabaseEntity;
import com.arangodb.entity.EntityFactory;
import com.arangodb.entity.StringsResultEntity;
import com.arangodb.entity.UserEntity;
import com.arangodb.http.HttpResponseEntity;

/**
 * @author tamtam180 - kirscheless at gmail.com
 *
 */
public class InternalDatabaseDriverImpl extends BaseArangoDriverImpl implements com.arangodb.InternalDatabaseDriver {

  InternalDatabaseDriverImpl(ArangoConfigure configure) {
    super(configure);
  }

  @Override
  public DatabaseEntity getCurrentDatabase() throws ArangoException {
    
    HttpResponseEntity res = httpManager.doGet(createEndpointUrl(baseUrl, null, "/_api/database/current"));
    return createEntity(res, DatabaseEntity.class);
    
  }
  
  @Override
  public StringsResultEntity getDatabases(boolean currentUserAccessableOnly, String username, String password) throws ArangoException {
    HttpResponseEntity res = httpManager.doGet(
        createEndpointUrl(baseUrl, null, "/_api/database", currentUserAccessableOnly ? "user" : null),
        null, null, username, password
        );
    return createEntity(res, StringsResultEntity.class);

  }
  
  @Override
  public BooleanResultEntity createDatabase(String database, UserEntity... users) throws ArangoException {

    validateDatabaseName(database, false);
    
    TreeMap<String, Object> body = new TreeMap<String, Object>();
    body.put("name", database);
    if (users != null && users.length > 0) {
      body.put("users", users);
    }
    
    HttpResponseEntity res = httpManager.doPost(
        createEndpointUrl(baseUrl, null, "/_api/database"),
        null, 
        EntityFactory.toJsonString(body)
        );
    
    return createEntity(res, BooleanResultEntity.class);
    
  }

  @Override
  public BooleanResultEntity deleteDatabase(String database) throws ArangoException {

    validateDatabaseName(database, false);
    
    TreeMap<String, Object> body = new TreeMap<String, Object>();
    body.put("name", database);
    
    HttpResponseEntity res = httpManager.doDelete(
        createEndpointUrl(baseUrl, null, "/_api/database", database),
        null
        );
    
    return createEntity(res, BooleanResultEntity.class);
    
  }

}
