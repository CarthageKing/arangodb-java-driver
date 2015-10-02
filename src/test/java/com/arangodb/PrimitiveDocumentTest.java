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

package com.arangodb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.entity.DocumentEntity;
import com.google.gson.Gson;

/**
 * @author tamtam180 - kirscheless at gmail.com
 * 
 */
public class PrimitiveDocumentTest extends BaseTest {

	private static Logger logger = LoggerFactory.getLogger(PrimitiveDocumentTest.class);

	public PrimitiveDocumentTest(ArangoConfigure configure, ArangoDriver driver) {
		super(configure, driver);
	}

	@Test
	public void test_string() throws ArangoException {

		String value = "AAA";
		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}
	}

	@Test
	public void test_string_quote() throws ArangoException {

		String value = "AA\"A";

		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}
	}

	@Test
	public void test_string_multibyte1() throws ArangoException {

		String value = "AA☆A";

		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}
	}

	@Test
	public void test_string_multibyte2() throws ArangoException {

		TestComplexEntity01 value = new TestComplexEntity01("寿司", "", 10);
		logger.debug(new Gson().toJson(value));

		DocumentEntity<?> res = driver.createDocument("unit_test_primitive", value, true, true);
		String documentHandle = res.getDocumentHandle();

		DocumentEntity<TestComplexEntity01> doc = driver.getDocument(documentHandle, TestComplexEntity01.class);
		logger.debug(doc.getEntity().getUser());
		logger.debug(doc.getEntity().getDesc());
		logger.debug(doc.getEntity().getAge().toString());
	}

	@Test
	public void test_string_escape() throws ArangoException {

		String value = "\\\\";
		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}
	}

	@Test
	public void test_string_spchar() throws ArangoException {

		String value = "AA\t\nA;/@*:='&%$#!~\\";

		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}
	}

	@Test
	public void test_null() throws ArangoException {

		String value = null;
		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}
	}

	@Test
	public void test_boolean_true() throws ArangoException {

		boolean value = true;

		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}

	}

	@Test
	public void test_boolean_false() throws ArangoException {

		boolean value = false;
		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}

	}

	@Test
	public void test_number_int() throws ArangoException {

		int value = 1000000;

		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}

	}

	@Test
	public void test_number_long() throws ArangoException {

		long value = Long.MAX_VALUE;

		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}

	}

	@Test
	@Ignore
	public void test_number_double() throws ArangoException {

		double value = Double.MAX_VALUE;

		try {
			driver.createDocument("unit_test_primitive", value, true, true);
			fail();
		} catch (ArangoException e) {
			assertThat(e.getErrorNumber(), is(ErrorNums.ERROR_ARANGO_DOCUMENT_TYPE_INVALID));
		}

	}

}
