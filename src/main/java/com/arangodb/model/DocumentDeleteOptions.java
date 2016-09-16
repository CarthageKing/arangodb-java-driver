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

package com.arangodb.model;

/**
 * @author Mark - mark at arangodb.com
 * 
 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#removes-a-document">API
 *      Documentation</a>
 */
public class DocumentDeleteOptions {

	private Boolean waitForSync;
	private String ifMatch;
	private Boolean returnOld;

	public DocumentDeleteOptions() {
		super();
	}

	public Boolean getWaitForSync() {
		return waitForSync;
	}

	/**
	 * @param waitForSync
	 *            Wait until deletion operation has been synced to disk.
	 * @return options
	 */
	public DocumentDeleteOptions waitForSync(final Boolean waitForSync) {
		this.waitForSync = waitForSync;
		return this;
	}

	public String getIfMatch() {
		return ifMatch;
	}

	/**
	 * @param ifMatch
	 *            remove a document based on a target revision
	 * @return options
	 */
	public DocumentDeleteOptions ifMatch(final String ifMatch) {
		this.ifMatch = ifMatch;
		return this;
	}

	public Boolean getReturnOld() {
		return returnOld;
	}

	/**
	 * @param returnOld
	 *            Return additionally the complete previous revision of the changed document under the attribute old in
	 *            the result.
	 * @return options
	 */
	public DocumentDeleteOptions returnOld(final Boolean returnOld) {
		this.returnOld = returnOld;
		return this;
	}

}
