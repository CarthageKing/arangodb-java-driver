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

package com.arangodb.example.document;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.arangodb.ArangoException;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionsEntity;

public class CollectionExample extends BaseExample {

	private static final String DATABASE_NAME = "CreateCollectionExample";

	@Before
	public void _before() {
		removeTestDatabase(DATABASE_NAME);

		createDatabase(driver, DATABASE_NAME);
	}

	@After
	public void _after() {
		removeTestDatabase(DATABASE_NAME);
	}

	@Test
	public void createAndDeleteCollection() {

		String myFirstCollection = "collection1";
		long myFirstCollectionId = 0L;

		//
		printHeadline("create a collection");
		//
		try {
			final CollectionEntity entity = driver.createCollection(myFirstCollection);
			Assert.assertNotNull(entity);
			Assert.assertNotNull(entity.getName());
			Assert.assertEquals(myFirstCollection, entity.getName());
			Assert.assertTrue(entity.getId() != 0L);
			myFirstCollectionId = entity.getId();

			printCollectionEntity(entity);
			// System.out.println(entity);
		} catch (final ArangoException e) {
			Assert.fail("create collection failed. " + e.getMessage());
		}

		createCollection(driver, "collection2");
		createCollection(driver, "collection3");

		//
		printHeadline("get list of all collections");
		//
		try {
			final CollectionsEntity collectionsEntity = driver.getCollections();
			Assert.assertNotNull(collectionsEntity);
			Assert.assertNotNull(collectionsEntity.getCollections());

			for (final CollectionEntity entity : collectionsEntity.getCollections()) {
				printCollectionEntity(entity);
				// System.out.println(entity);
			}

		} catch (final ArangoException e) {
			Assert.fail("could not get collections. " + e.getMessage());
		}

		//
		printHeadline("get one collection");
		//
		try {
			final CollectionEntity entity = driver.getCollection(myFirstCollection);
			Assert.assertNotNull(entity);
			Assert.assertNotNull(entity.getName());
			Assert.assertNotNull(entity.getId());

			printCollectionEntity(entity);
			// System.out.println(entity);
		} catch (final ArangoException e) {
			Assert.fail("could not get collection. " + e.getMessage());
		}

		//
		printHeadline("rename collection");
		//
		try {
			final CollectionEntity entity = driver.renameCollection(myFirstCollection, "collection4");
			Assert.assertNotNull(entity);
			Assert.assertNotNull(entity.getName());
			Assert.assertNotNull(entity.getId());
			Assert.assertEquals(myFirstCollectionId, entity.getId());
			myFirstCollection = entity.getName();

			printCollectionEntity(entity);
			// System.out.println(entity);
		} catch (final ArangoException e) {
			Assert.fail("could not rename collection. " + e.getMessage());
		}

		//
		printHeadline("truncate collection");
		//
		try {
			final CollectionEntity truncateCollection = driver.truncateCollection(myFirstCollection);
			Assert.assertNotNull(truncateCollection);
			Assert.assertNotNull(truncateCollection.getName());
			Assert.assertNotNull(truncateCollection.getId());
			Assert.assertEquals(myFirstCollectionId, truncateCollection.getId());

		} catch (final ArangoException e) {
			Assert.fail("could not truncate collection. " + e.getMessage());
		}

		//
		printHeadline("delete collection");
		//
		try {
			final CollectionEntity entity = driver.deleteCollection(myFirstCollection);
			Assert.assertNotNull(entity);
			// the name has to be null
			Assert.assertNull(entity.getName());
			Assert.assertNotNull(entity.getId());
			Assert.assertEquals(myFirstCollectionId, entity.getId());

			printCollectionEntity(entity);
			// System.out.println(entity);
		} catch (final ArangoException e) {
			Assert.fail("could not delete collection. " + e.getMessage());
		}

	}

	private void printCollectionEntity(final CollectionEntity collection) {
		if (collection == null) {
			System.out.println("Collection not found");
		} else if (collection.getName() == null) {
			// collection is deleted
			System.out.println("Collection '" + collection.getName() + "' with id '" + collection.getId() + "'");
		} else {
			System.out.println(
				"Collection '" + collection.getName() + "' (" + (collection.getIsSystem() ? "system" : "normal")
						+ " collection) with id '" + collection.getId() + "'");
		}
	}

}
