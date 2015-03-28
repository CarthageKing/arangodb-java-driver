/**
 * Copyright 2004-2015 triAGENS GmbH, Cologne, Germany
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is triAGENS GmbH, Cologne, Germany
 *
 * @author a-brandt
 * @author Copyright 2015, triAGENS GmbH, Cologne, Germany
 */

package com.arangodb;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.arangodb.InternalTraversalDriver.Direction;
import com.arangodb.InternalTraversalDriver.ItemOrder;
import com.arangodb.InternalTraversalDriver.Order;
import com.arangodb.InternalTraversalDriver.Strategy;
import com.arangodb.InternalTraversalDriver.Uniqueness;
import com.arangodb.entity.BaseEntity;
import com.arangodb.entity.EdgeDefinitionEntity;
import com.arangodb.entity.TraversalEntity;
import com.arangodb.entity.marker.VertexEntity;

/**
 * @author a-brandt
 */
public class ArangoDriverTraversalTest extends BaseGraphTest {

	String graphName = "TraversalTestGraph";
	String vertexCollectionName = "person";
	String edgeCollectionName = "knows";

	public ArangoDriverTraversalTest(ArangoConfigure configure, ArangoDriver driver) {
		super(configure, driver);
	}

	@Before
	public void _before() throws ArangoException {
		super._before();

		List<EdgeDefinitionEntity> edgeDefinitions = new ArrayList<EdgeDefinitionEntity>();
		EdgeDefinitionEntity edgeDefinition = new EdgeDefinitionEntity();
		edgeDefinition.setCollection(edgeCollectionName);

		List<String> from = new ArrayList<String>();
		from.add(vertexCollectionName);
		edgeDefinition.setFrom(from);

		List<String> to = new ArrayList<String>();
		to.add(vertexCollectionName);
		edgeDefinition.setTo(to);

		edgeDefinitions.add(edgeDefinition);

		driver.createGraph(graphName, edgeDefinitions, null, true);

		VertexEntity<Person> alice = createPerson("Alice");
		VertexEntity<Person> bob = createPerson("Bob");
		VertexEntity<Person> charlie = createPerson("Charlie");
		VertexEntity<Person> dave = createPerson("Dave");
		VertexEntity<Person> eve = createPerson("Eve");

		driver.graphCreateEdge(graphName, edgeCollectionName, "Alice_knows_Bob", alice.getDocumentHandle(),
			bob.getDocumentHandle());
		driver.graphCreateEdge(graphName, edgeCollectionName, "Bob_knows_Charlie", bob.getDocumentHandle(),
			charlie.getDocumentHandle());
		driver.graphCreateEdge(graphName, edgeCollectionName, "Bob_knows_Dave", bob.getDocumentHandle(),
			dave.getDocumentHandle());
		driver.graphCreateEdge(graphName, edgeCollectionName, "Eve_knows_Alice", eve.getDocumentHandle(),
			alice.getDocumentHandle());
		driver.graphCreateEdge(graphName, edgeCollectionName, "Eve_knows_Bob", eve.getDocumentHandle(),
			bob.getDocumentHandle());
	}

	@Test
	public void test_create_vertex() throws ArangoException {
		String edgeCollection = null;
		String startVertex = "person/Alice";
		String filter = null;
		Long minDepth = null;
		Long maxDepth = null;
		String visitor = null;
		Direction direction = Direction.OUTBOUND;
		String init = null;
		String expander = null;
		String sort = null;
		Strategy strategy = null;
		Order order = null;
		ItemOrder itemOrder = null;
		Uniqueness verticesUniqueness = null;
		Uniqueness edgesUniqueness = null;
		Long maxIterations = null;

		TraversalEntity<Object, Object> traversal = driver.getTraversal(graphName, edgeCollection, startVertex, null,
			null, filter, minDepth, maxDepth, visitor, direction, init, expander, sort, strategy, order, itemOrder,
			verticesUniqueness, edgesUniqueness, maxIterations);

	}

	private VertexEntity<Person> createPerson(String name) throws ArangoException {
		return driver.graphCreateVertex(graphName, vertexCollectionName, name, new Person(name), true);
	}

	public class Person extends BaseEntity {

		private String name;

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}
}
