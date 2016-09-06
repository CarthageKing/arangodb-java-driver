package com.arangodb;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.EdgeResult;
import com.arangodb.entity.EdgeUpdateResult;
import com.arangodb.entity.VertexResult;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.EdgeReplaceOptions;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoEdgeCollectionTest extends BaseTest {

	private static final String GRAPH_NAME = "db_collection_test";
	private static final String EDGE_COLLECTION_NAME = "db_edge_collection_test";
	private static final String VERTEX_COLLECTION_NAME = "db_vertex_collection_test";

	@Before
	public void setup() {
		try {
			db.createCollection(VERTEX_COLLECTION_NAME, null);
		} catch (final ArangoDBException e) {
		}
		try {
			db.createCollection(EDGE_COLLECTION_NAME, new CollectionCreateOptions().type(CollectionType.EDGES));
		} catch (final ArangoDBException e) {
		}
		final Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
		edgeDefinitions.add(new EdgeDefinition().collection(EDGE_COLLECTION_NAME).from(VERTEX_COLLECTION_NAME)
				.to(VERTEX_COLLECTION_NAME));
		db.createGraph(GRAPH_NAME, edgeDefinitions, null);
	}

	@After
	public void teardown() {
		Stream.of(VERTEX_COLLECTION_NAME, EDGE_COLLECTION_NAME).forEach(collection -> {
			try {
				db.collection(collection).drop();
			} catch (final ArangoDBException e) {
			}
		});
		db.graph(GRAPH_NAME).drop();
	}

	private BaseEdgeDocument createEdgeValue() {
		final VertexResult v1 = db.graph(GRAPH_NAME).vertexCollection(VERTEX_COLLECTION_NAME)
				.insertVertex(new BaseDocument(), null);
		final VertexResult v2 = db.graph(GRAPH_NAME).vertexCollection(VERTEX_COLLECTION_NAME)
				.insertVertex(new BaseDocument(), null);

		final BaseEdgeDocument value = new BaseEdgeDocument();
		value.setFrom(v1.getId());
		value.setTo(v2.getId());
		return value;
	}

	@Test
	public void insertEdge() {
		final BaseEdgeDocument value = createEdgeValue();
		final EdgeResult edge = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(value, null);
		assertThat(edge, is(notNullValue()));
		final BaseDocument document = db.collection(EDGE_COLLECTION_NAME).getDocument(edge.getKey(), BaseDocument.class,
			null);
		assertThat(document, is(notNullValue()));
		assertThat(document.getKey(), is(edge.getKey()));
	}

	@Test
	public void getEdge() {
		final BaseEdgeDocument value = createEdgeValue();
		final EdgeResult edge = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(value, null);
		final BaseDocument document = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).getEdge(edge.getKey(),
			BaseDocument.class, null);
		assertThat(document, is(notNullValue()));
		assertThat(document.getKey(), is(edge.getKey()));
	}

	@Test
	public void getEdgeIfMatch() {
		final BaseEdgeDocument value = createEdgeValue();
		final EdgeResult edge = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(value, null);
		final DocumentReadOptions options = new DocumentReadOptions().ifMatch(edge.getRev());
		final BaseDocument document = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).getEdge(edge.getKey(),
			BaseDocument.class, options);
		assertThat(document, is(notNullValue()));
		assertThat(document.getKey(), is(edge.getKey()));
	}

	@Test
	public void getEdgeIfMatchFail() {
		final BaseEdgeDocument value = createEdgeValue();
		final EdgeResult edge = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(value, null);
		final DocumentReadOptions options = new DocumentReadOptions().ifMatch("no");
		try {
			db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).getEdge(edge.getKey(), BaseDocument.class,
				options);
			fail();
		} catch (final ArangoDBException e) {
		}
	}

	@Test
	public void getEdgeIfNoneMatch() {
		final BaseEdgeDocument value = createEdgeValue();
		final EdgeResult edge = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(value, null);
		final DocumentReadOptions options = new DocumentReadOptions().ifNoneMatch("no");
		final BaseDocument document = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).getEdge(edge.getKey(),
			BaseDocument.class, options);
		assertThat(document, is(notNullValue()));
		assertThat(document.getKey(), is(edge.getKey()));
	}

	@Test
	public void getEdgeIfNoneMatchFail() {
		final BaseEdgeDocument value = createEdgeValue();
		final EdgeResult edge = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(value, null);
		final DocumentReadOptions options = new DocumentReadOptions().ifNoneMatch(edge.getRev());
		try {
			db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).getEdge(edge.getKey(), BaseDocument.class,
				options);
			fail();
		} catch (final ArangoDBException e) {
		}
	}

	@Test
	public void replaceEdge() {
		final BaseEdgeDocument doc = createEdgeValue();
		doc.addAttribute("a", "test");
		final EdgeResult createResult = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(doc, null);
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final EdgeUpdateResult replaceResult = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME)
				.replaceEdge(createResult.getKey(), doc, null);
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getRev(), is(not(replaceResult.getOldRev())));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME)
				.getEdge(createResult.getKey(), BaseDocument.class, null);
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getRevision(), is(replaceResult.getRev()));
		assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
		assertThat(readResult.getAttribute("b"), is("test"));
	}

	@Test
	public void replaceEdgeIfMatch() {
		final BaseEdgeDocument doc = createEdgeValue();
		doc.addAttribute("a", "test");
		final EdgeResult createResult = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(doc, null);
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final EdgeReplaceOptions options = new EdgeReplaceOptions().ifMatch(createResult.getRev());
		final EdgeUpdateResult replaceResult = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME)
				.replaceEdge(createResult.getKey(), doc, options);
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getRev(), is(not(replaceResult.getOldRev())));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME)
				.getEdge(createResult.getKey(), BaseDocument.class, null);
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getRevision(), is(replaceResult.getRev()));
		assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
		assertThat(readResult.getAttribute("b"), is("test"));
	}

	@Test
	public void replaceEdgeIfMatchFail() {
		final BaseEdgeDocument doc = createEdgeValue();
		doc.addAttribute("a", "test");
		final EdgeResult createResult = db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(doc, null);
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		try {
			final EdgeReplaceOptions options = new EdgeReplaceOptions().ifMatch("no");
			db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).replaceEdge(createResult.getKey(), doc, options);
			fail();
		} catch (final ArangoDBException e) {
		}
	}

}
