package com.arangodb.model;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arangodb.ArangoDBException;
import com.arangodb.BaseTest;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.CollectionPropertiesResult;
import com.arangodb.entity.CollectionResult;
import com.arangodb.entity.DocumentCreateResult;
import com.arangodb.entity.DocumentUpdateResult;
import com.arangodb.entity.IndexResult;
import com.arangodb.entity.IndexType;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class DBCollectionTest extends BaseTest {

	private static final String COLLECTION_NAME = "db_collection_test";

	@Before
	public void setup() {
		db.createCollection(COLLECTION_NAME, null).execute();
	}

	@After
	public void teardown() {
		db.deleteCollection(COLLECTION_NAME).execute();
	}

	@Test
	public void createDocument() {
		final DocumentCreateResult<BaseDocument> doc = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), null).execute();
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getId(), is(notNullValue()));
		assertThat(doc.getKey(), is(notNullValue()));
		assertThat(doc.getRev(), is(notNullValue()));
		assertThat(doc.getNew().isPresent(), is(false));
		assertThat(doc.getId(), is(COLLECTION_NAME + "/" + doc.getKey()));
	}

	@Test
	public void createDocumentAsync() throws InterruptedException, ExecutionException {
		final CompletableFuture<DocumentCreateResult<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), null).executeAsync();
		assertThat(f, is(notNullValue()));
		f.whenComplete((doc, ex) -> {
			assertThat(ex, is(nullValue()));
			assertThat(doc.getId(), is(notNullValue()));
			assertThat(doc.getKey(), is(notNullValue()));
			assertThat(doc.getRev(), is(notNullValue()));
			assertThat(doc.getNew().isPresent(), is(false));
		});
		f.get();
	}

	@Test
	public void createDocumentReturnNew() {
		final DocumentCreate.Options options = new DocumentCreate.Options().returnNew(true);
		final DocumentCreateResult<BaseDocument> doc = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), options).execute();
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getId(), is(notNullValue()));
		assertThat(doc.getKey(), is(notNullValue()));
		assertThat(doc.getRev(), is(notNullValue()));
		assertThat(doc.getNew().isPresent(), is(true));
	}

	@Test
	public void createDocumentWaitForSync() {
		final DocumentCreate.Options options = new DocumentCreate.Options().waitForSync(true);
		final DocumentCreateResult<BaseDocument> doc = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), options).execute();
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getId(), is(notNullValue()));
		assertThat(doc.getKey(), is(notNullValue()));
		assertThat(doc.getRev(), is(notNullValue()));
		assertThat(doc.getNew().isPresent(), is(false));
	}

	@Test
	public void createDocumentAsJson() {
		final DocumentCreateResult<String> doc = db.collection(COLLECTION_NAME)
				.createDocument("{\"_key\":\"docRaw\",\"a\":\"test\"}", null).execute();
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getId(), is(notNullValue()));
		assertThat(doc.getKey(), is(notNullValue()));
		assertThat(doc.getRev(), is(notNullValue()));
	}

	@Test
	public void readDocument() {
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), null).execute();
		assertThat(createResult.getKey(), is(notNullValue()));
		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getId(), is(COLLECTION_NAME + "/" + createResult.getKey()));
	}

	@Test
	public void readDocumentIfMatch() {
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), null).execute();
		assertThat(createResult.getKey(), is(notNullValue()));
		final DocumentRead.Options options = new DocumentRead.Options().ifMatch(createResult.getRev());
		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, options).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getId(), is(COLLECTION_NAME + "/" + createResult.getKey()));
	}

	@Test
	public void readDocumentIfMatchFail() {
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), null).execute();
		assertThat(createResult.getKey(), is(notNullValue()));
		final DocumentRead.Options options = new DocumentRead.Options().ifMatch("no");
		try {
			db.collection(COLLECTION_NAME).readDocument(createResult.getKey(), BaseDocument.class, options).execute();
			fail();
		} catch (final ArangoDBException e) {
			assertThat(e.getCode().isPresent(), is(true));
			assertThat(e.getCode().get(), is(greaterThan(200)));
			assertThat(e.getErrorNum().isPresent(), is(true));
			assertThat(e.getErrorMessage().isPresent(), is(true));
		}
	}

	@Test
	public void readDocumentIfNoneMatch() {
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), null).execute();
		assertThat(createResult.getKey(), is(notNullValue()));
		final DocumentRead.Options options = new DocumentRead.Options().ifNoneMatch("no");
		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, options).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getId(), is(COLLECTION_NAME + "/" + createResult.getKey()));
	}

	@Test
	public void readDocumentIfNoneMatchFail() {
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.createDocument(new BaseDocument(), null).execute();
		assertThat(createResult.getKey(), is(notNullValue()));
		final DocumentRead.Options options = new DocumentRead.Options().ifNoneMatch(createResult.getRev());
		try {
			db.collection(COLLECTION_NAME).readDocument(createResult.getKey(), BaseDocument.class, options).execute();
			fail();
		} catch (final ArangoDBException e) {
		}
	}

	@Test
	public void readDocumentAsJson() {
		db.collection(COLLECTION_NAME).createDocument("{\"_key\":\"docRaw\",\"a\":\"test\"}", null).execute();
		final String readResult = db.collection(COLLECTION_NAME).readDocument("docRaw", String.class, null).execute();
		assertThat(readResult.contains("\"_key\":\"docRaw\""), is(true));
		assertThat(readResult.contains("\"_id\":\"db_collection_test\\/docRaw\""), is(true));
	}

	@Test
	public void updateDocument() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		doc.addAttribute("c", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		doc.updateAttribute("c", null);
		final DocumentUpdateResult<BaseDocument> updateResult = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, null).execute();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getNew().isPresent(), is(false));
		assertThat(updateResult.getOld().isPresent(), is(false));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getAttribute("a"), is("test1"));
		assertThat(readResult.getAttribute("b"), is("test"));
		assertThat(readResult.getRevision(), is(updateResult.getRev()));
		assertThat(readResult.getProperties().keySet(), hasItem("c"));
	}

	@Test
	public void updateDocumentIfMatch() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		doc.addAttribute("c", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		doc.updateAttribute("c", null);
		final DocumentUpdate.Options options = new DocumentUpdate.Options().ifMatch(createResult.getRev());
		final DocumentUpdateResult<BaseDocument> updateResult = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options).execute();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getAttribute("a"), is("test1"));
		assertThat(readResult.getAttribute("b"), is("test"));
		assertThat(readResult.getRevision(), is(updateResult.getRev()));
		assertThat(readResult.getProperties().keySet(), hasItem("c"));
	}

	@Test
	public void updateDocumentIfMatchFail() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		doc.addAttribute("c", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		doc.updateAttribute("c", null);
		try {
			final DocumentUpdate.Options options = new DocumentUpdate.Options().ifMatch("no");
			db.collection(COLLECTION_NAME).updateDocument(createResult.getKey(), doc, options).execute();
			fail();
		} catch (final ArangoDBException e) {
			assertThat(e.getCode().isPresent(), is(true));
			assertThat(e.getCode().get(), is(greaterThan(200)));
			assertThat(e.getErrorNum().isPresent(), is(true));
			assertThat(e.getErrorMessage().isPresent(), is(true));
		}
	}

	@Test
	public void updateDocumentReturnNew() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		final DocumentUpdate.Options options = new DocumentUpdate.Options().returnNew(true);
		final DocumentUpdateResult<BaseDocument> updateResult = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options).execute();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));
		assertThat(updateResult.getNew().isPresent(), is(true));
		assertThat(updateResult.getNew().get().getKey(), is(createResult.getKey()));
		assertThat(updateResult.getNew().get().getRevision(), is(not(createResult.getRev())));
		assertThat(updateResult.getNew().get().getAttribute("a"), is("test1"));
		assertThat(updateResult.getNew().get().getAttribute("b"), is("test"));
	}

	@Test
	public void updateDocumentReturnOld() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		final DocumentUpdate.Options options = new DocumentUpdate.Options().returnOld(true);
		final DocumentUpdateResult<BaseDocument> updateResult = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options).execute();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));
		assertThat(updateResult.getOld().isPresent(), is(true));
		assertThat(updateResult.getOld().get().getKey(), is(createResult.getKey()));
		assertThat(updateResult.getOld().get().getRevision(), is(createResult.getRev()));
		assertThat(updateResult.getOld().get().getAttribute("a"), is("test"));
		assertThat(updateResult.getOld().get().getProperties().keySet(), not(hasItem("b")));
	}

	@Test
	public void updateDocumentKeepNullTrue() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.updateAttribute("a", null);
		final DocumentUpdate.Options options = new DocumentUpdate.Options().keepNull(true);
		final DocumentUpdateResult<BaseDocument> updateResult = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options).execute();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getProperties().keySet(), hasItem("a"));
	}

	@Test
	public void updateDocumentKeepNullFalse() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.updateAttribute("a", null);
		final DocumentUpdate.Options options = new DocumentUpdate.Options().keepNull(false);
		final DocumentUpdateResult<BaseDocument> updateResult = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options).execute();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getId(), is(createResult.getId()));
		assertThat(readResult.getRevision(), is(notNullValue()));
		assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updateDocumentMergeObjectsTrue() {
		final BaseDocument doc = new BaseDocument();
		final Map<String, String> a = new HashMap<>();
		a.put("a", "test");
		doc.addAttribute("a", a);
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		a.clear();
		a.put("b", "test");
		doc.updateAttribute("a", a);
		final DocumentUpdate.Options options = new DocumentUpdate.Options().mergeObjects(true);
		final DocumentUpdateResult<BaseDocument> updateResult = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options).execute();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		final Object aResult = readResult.getAttribute("a");
		assertThat(aResult, instanceOf(Map.class));
		final Map<String, String> aMap = (Map<String, String>) aResult;
		assertThat(aMap.keySet(), hasItem("a"));
		assertThat(aMap.keySet(), hasItem("b"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updateDocumentMergeObjectsFalse() {
		final BaseDocument doc = new BaseDocument();
		final Map<String, String> a = new HashMap<>();
		a.put("a", "test");
		doc.addAttribute("a", a);
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		a.clear();
		a.put("b", "test");
		doc.updateAttribute("a", a);
		final DocumentUpdate.Options options = new DocumentUpdate.Options().mergeObjects(false);
		final DocumentUpdateResult<BaseDocument> updateResult = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options).execute();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		final Object aResult = readResult.getAttribute("a");
		assertThat(aResult, instanceOf(Map.class));
		final Map<String, String> aMap = (Map<String, String>) aResult;
		assertThat(aMap.keySet(), not(hasItem("a")));
		assertThat(aMap.keySet(), hasItem("b"));
	}

	@Test
	public void updateDocumentIgnoreRevsFalse() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.setRevision("no");
		doc.updateAttribute("a", "test1");
		try {
			final DocumentUpdate.Options options = new DocumentUpdate.Options().ignoreRevs(false);
			db.collection(COLLECTION_NAME).updateDocument(createResult.getKey(), doc, options).execute();
			fail();
		} catch (final ArangoDBException e) {
			assertThat(e.getCode().isPresent(), is(true));
			assertThat(e.getCode().get(), is(greaterThan(200)));
			assertThat(e.getErrorNum().isPresent(), is(true));
			assertThat(e.getErrorMessage().isPresent(), is(true));
		}
	}

	@Test
	public void replaceDocumemt() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final DocumentUpdateResult<BaseDocument> replaceResult = db.collection(COLLECTION_NAME)
				.replaceDocument(createResult.getKey(), doc, null).execute();
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getNew().isPresent(), is(false));
		assertThat(replaceResult.getOld().isPresent(), is(false));
		assertThat(replaceResult.getRev(), is(not(replaceResult.getOldRev())));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getRevision(), is(replaceResult.getRev()));
		assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
		assertThat(readResult.getAttribute("b"), is("test"));
	}

	@Test
	public void replaceDocumemtIfMatch() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final DocumentReplace.Options options = new DocumentReplace.Options().ifMatch(createResult.getRev());
		final DocumentUpdateResult<BaseDocument> replaceResult = db.collection(COLLECTION_NAME)
				.replaceDocument(createResult.getKey(), doc, options).execute();
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getRev(), is(not(replaceResult.getOldRev())));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(createResult.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getRevision(), is(replaceResult.getRev()));
		assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
		assertThat(readResult.getAttribute("b"), is("test"));
	}

	@Test
	public void replaceDocumemtIfMatchFail() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		try {
			final DocumentReplace.Options options = new DocumentReplace.Options().ifMatch("no");
			db.collection(COLLECTION_NAME).replaceDocument(createResult.getKey(), doc, options).execute();
		} catch (final ArangoDBException e) {
			assertThat(e.getCode().isPresent(), is(true));
			assertThat(e.getCode().get(), is(greaterThan(200)));
			assertThat(e.getErrorNum().isPresent(), is(true));
			assertThat(e.getErrorMessage().isPresent(), is(true));
		}
	}

	@Test
	public void replaceDocumemtIgnoreRevsFalse() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.setRevision("no");
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		try {
			final DocumentReplace.Options options = new DocumentReplace.Options().ignoreRevs(false);
			db.collection(COLLECTION_NAME).replaceDocument(createResult.getKey(), doc, options).execute();
		} catch (final ArangoDBException e) {
			assertThat(e.getCode().isPresent(), is(true));
			assertThat(e.getCode().get(), is(greaterThan(200)));
			assertThat(e.getErrorNum().isPresent(), is(true));
			assertThat(e.getErrorMessage().isPresent(), is(true));
		}
	}

	@Test
	public void replaceDocumentReturnNew() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final DocumentReplace.Options options = new DocumentReplace.Options().returnNew(true);
		final DocumentUpdateResult<BaseDocument> replaceResult = db.collection(COLLECTION_NAME)
				.replaceDocument(createResult.getKey(), doc, options).execute();
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));
		assertThat(replaceResult.getNew().isPresent(), is(true));
		assertThat(replaceResult.getNew().get().getKey(), is(createResult.getKey()));
		assertThat(replaceResult.getNew().get().getRevision(), is(not(createResult.getRev())));
		assertThat(replaceResult.getNew().get().getProperties().keySet(), not(hasItem("a")));
		assertThat(replaceResult.getNew().get().getAttribute("b"), is("test"));
	}

	@Test
	public void replaceDocumentReturnOld() {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateResult<BaseDocument> createResult = db.collection(COLLECTION_NAME).createDocument(doc, null)
				.execute();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final DocumentReplace.Options options = new DocumentReplace.Options().returnOld(true);
		final DocumentUpdateResult<BaseDocument> replaceResult = db.collection(COLLECTION_NAME)
				.replaceDocument(createResult.getKey(), doc, options).execute();
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));
		assertThat(replaceResult.getOld().isPresent(), is(true));
		assertThat(replaceResult.getOld().get().getKey(), is(createResult.getKey()));
		assertThat(replaceResult.getOld().get().getRevision(), is(createResult.getRev()));
		assertThat(replaceResult.getOld().get().getAttribute("a"), is("test"));
		assertThat(replaceResult.getOld().get().getProperties().keySet(), not(hasItem("b")));
	}

	@Test
	public void createHashIndex() {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		fields.add("b");
		final IndexResult indexResult = db.collection(COLLECTION_NAME).createHashIndex(fields, null).execute();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint().isPresent(), is(false));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getFields(), hasItem("b"));
		assertThat(indexResult.getGeoJson().isPresent(), is(false));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated().isPresent(), is(true));
		assertThat(indexResult.getIsNewlyCreated().get(), is(true));
		assertThat(indexResult.getMinLength().isPresent(), is(false));
		assertThat(indexResult.getSelectivityEstimate().isPresent(), is(true));
		assertThat(indexResult.getSelectivityEstimate().get(), is(1));
		assertThat(indexResult.getSparse().isPresent(), is(true));
		assertThat(indexResult.getSparse().get(), is(false));
		assertThat(indexResult.getType(), is(IndexType.hash));
		assertThat(indexResult.getUnique().isPresent(), is(true));
		assertThat(indexResult.getUnique().get(), is(false));
	}

	@Test
	public void createGeoIndex() {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		final IndexResult indexResult = db.collection(COLLECTION_NAME).createGeoIndex(fields, null).execute();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint().isPresent(), is(true));
		assertThat(indexResult.getConstraint().get(), is(false));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getGeoJson().isPresent(), is(true));
		assertThat(indexResult.getGeoJson().get(), is(false));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated().isPresent(), is(true));
		assertThat(indexResult.getIsNewlyCreated().get(), is(true));
		assertThat(indexResult.getMinLength().isPresent(), is(false));
		assertThat(indexResult.getSelectivityEstimate().isPresent(), is(false));
		assertThat(indexResult.getSparse().isPresent(), is(true));
		assertThat(indexResult.getSparse().get(), is(true));
		assertThat(indexResult.getType(), is(IndexType.geo1));
		assertThat(indexResult.getUnique().isPresent(), is(true));
		assertThat(indexResult.getUnique().get(), is(false));
	}

	@Test
	public void createGeo2Index() {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		fields.add("b");
		final IndexResult indexResult = db.collection(COLLECTION_NAME).createGeoIndex(fields, null).execute();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint().isPresent(), is(true));
		assertThat(indexResult.getConstraint().get(), is(false));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getFields(), hasItem("b"));
		assertThat(indexResult.getGeoJson().isPresent(), is(false));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated().isPresent(), is(true));
		assertThat(indexResult.getIsNewlyCreated().get(), is(true));
		assertThat(indexResult.getMinLength().isPresent(), is(false));
		assertThat(indexResult.getSelectivityEstimate().isPresent(), is(false));
		assertThat(indexResult.getSparse().isPresent(), is(true));
		assertThat(indexResult.getSparse().get(), is(true));
		assertThat(indexResult.getType(), is(IndexType.geo2));
		assertThat(indexResult.getUnique().isPresent(), is(true));
		assertThat(indexResult.getUnique().get(), is(false));
	}

	@Test
	public void createSkiplistIndex() {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		fields.add("b");
		final IndexResult indexResult = db.collection(COLLECTION_NAME).createSkiplistIndex(fields, null).execute();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint().isPresent(), is(false));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getFields(), hasItem("b"));
		assertThat(indexResult.getGeoJson().isPresent(), is(false));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated().isPresent(), is(true));
		assertThat(indexResult.getIsNewlyCreated().get(), is(true));
		assertThat(indexResult.getMinLength().isPresent(), is(false));
		assertThat(indexResult.getSelectivityEstimate().isPresent(), is(false));
		assertThat(indexResult.getSparse().isPresent(), is(true));
		assertThat(indexResult.getSparse().get(), is(false));
		assertThat(indexResult.getType(), is(IndexType.skiplist));
		assertThat(indexResult.getUnique().isPresent(), is(true));
		assertThat(indexResult.getUnique().get(), is(false));
	}

	@Test
	public void createPersistentIndex() {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		fields.add("b");
		final IndexResult indexResult = db.collection(COLLECTION_NAME).createPersistentIndex(fields, null).execute();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint().isPresent(), is(false));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getFields(), hasItem("b"));
		assertThat(indexResult.getGeoJson().isPresent(), is(false));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated().isPresent(), is(true));
		assertThat(indexResult.getIsNewlyCreated().get(), is(true));
		assertThat(indexResult.getMinLength().isPresent(), is(false));
		assertThat(indexResult.getSelectivityEstimate().isPresent(), is(false));
		assertThat(indexResult.getSparse().isPresent(), is(true));
		assertThat(indexResult.getSparse().get(), is(false));
		assertThat(indexResult.getType(), is(IndexType.persistent));
		assertThat(indexResult.getUnique().isPresent(), is(true));
		assertThat(indexResult.getUnique().get(), is(false));
	}

	@Test
	public void readIndexes() {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		db.collection(COLLECTION_NAME).createHashIndex(fields, null).execute();
		final Collection<IndexResult> indexes = db.collection(COLLECTION_NAME).readIndexes().execute();
		assertThat(indexes, is(notNullValue()));
		assertThat(indexes.size(), is(2));
		indexes.stream().forEach((i) -> {
			assertThat(i.getType(), anyOf(is(IndexType.primary), is(IndexType.hash)));
			if (i.getType() == IndexType.hash) {
				assertThat(i.getFields().size(), is(1));
				assertThat(i.getFields(), hasItem("a"));
			}
		});
	}

	@Test
	public void truncate() {
		final BaseDocument doc = new BaseDocument();
		db.collection(COLLECTION_NAME).createDocument(doc, null).execute();
		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.readDocument(doc.getKey(), BaseDocument.class, null).execute();
		assertThat(readResult.getKey(), is(doc.getKey()));
		final CollectionResult truncateResult = db.collection(COLLECTION_NAME).truncate().execute();
		assertThat(truncateResult, is(notNullValue()));
		assertThat(truncateResult.getId(), is(notNullValue()));
		try {
			db.collection(COLLECTION_NAME).readDocument(doc.getKey(), BaseDocument.class, null).execute();
			fail();
		} catch (final ArangoDBException e) {
			assertThat(e.getCode().isPresent(), is(true));
			assertThat(e.getCode().get(), is(404));
		}
	}

	@Test
	public void getCount() {
		final CollectionPropertiesResult countEmpty = db.collection(COLLECTION_NAME).getCount().execute();
		assertThat(countEmpty, is(notNullValue()));
		assertThat(countEmpty.getCount(), is(0L));
		db.collection(COLLECTION_NAME).createDocument("{}", null).execute();
		final CollectionPropertiesResult count = db.collection(COLLECTION_NAME).getCount().execute();
		assertThat(count.getCount(), is(1L));
	}

	@Test
	public void documentExists() {
		final Boolean existsNot = db.collection(COLLECTION_NAME).documentExists("no", null).execute();
		assertThat(existsNot, is(false));
		db.collection(COLLECTION_NAME).createDocument("{\"_key\":\"abc\"}", null).execute();
		final Boolean exists = db.collection(COLLECTION_NAME).documentExists("abc", null).execute();
		assertThat(exists, is(true));
	}

	@Test
	public void documentExistsAsync() throws Exception {
		final CompletableFuture<Boolean> existsNot = db.collection(COLLECTION_NAME).documentExists("no", null)
				.executeAsync();
		existsNot.thenAccept(result -> {
			assertThat(result, is(false));
		});
		existsNot.get();
		db.collection(COLLECTION_NAME).createDocument("{\"_key\":\"abc\"}", null).execute();
		final CompletableFuture<Boolean> exists = db.collection(COLLECTION_NAME).documentExists("abc", null)
				.executeAsync();
		exists.thenAccept(result -> {
			assertThat(result, is(true));
		});
	}

	@Test
	public void documentExistsIfMatch() {
		final DocumentCreateResult<String> createResult = db.collection(COLLECTION_NAME)
				.createDocument("{\"_key\":\"abc\"}", null).execute();
		final DocumentExists.Options options = new DocumentExists.Options().ifMatch(createResult.getRev());
		final Boolean exists = db.collection(COLLECTION_NAME).documentExists("abc", options).execute();
		assertThat(exists, is(true));
	}

	@Test
	public void documentExistsIfMatchFail() {
		db.collection(COLLECTION_NAME).createDocument("{\"_key\":\"abc\"}", null).execute();
		final DocumentExists.Options options = new DocumentExists.Options().ifMatch("no");
		final Boolean exists = db.collection(COLLECTION_NAME).documentExists("abc", options).execute();
		assertThat(exists, is(false));
	}

	@Test
	public void documentExistsIfNoneMatch() {
		db.collection(COLLECTION_NAME).createDocument("{\"_key\":\"abc\"}", null).execute();
		final DocumentExists.Options options = new DocumentExists.Options().ifNoneMatch("no");
		final Boolean exists = db.collection(COLLECTION_NAME).documentExists("abc", options).execute();
		assertThat(exists, is(true));
	}

	@Test
	public void documentExistsIfNoneMatchFail() {
		final DocumentCreateResult<String> createResult = db.collection(COLLECTION_NAME)
				.createDocument("{\"_key\":\"abc\"}", null).execute();
		final DocumentExists.Options options = new DocumentExists.Options().ifNoneMatch(createResult.getRev());
		final Boolean exists = db.collection(COLLECTION_NAME).documentExists("abc", options).execute();
		assertThat(exists, is(false));
	}

}
