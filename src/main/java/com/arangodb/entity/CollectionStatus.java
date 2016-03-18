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

package com.arangodb.entity;

import java.io.Serializable;
import java.util.TreeMap;

/**
 * Enumeration describing the collection state
 *
 * @author tamtam180 - kirscheless at gmail.com
 *
 */
public enum CollectionStatus {

	/**
	 * initial state of a new collection
	 */
	NEW_BORN_COLLECTION(1),

	/**
	 * collection is not in memory
	 */
	UNLOADED(2),

	/**
	 * collection is in memory
	 */
	LOADED(3),

	/**
	 * temporary state of a collection in the process of being unloaded
	 */
	IN_THE_PROCESS_OF_BEING_UNLOADED(4),

	/**
	 * deleted state
	 */
	DELETED(5);

	private static class Holder implements Serializable {
		private static final long serialVersionUID = -7016368432042468015L;
		private static TreeMap<Integer, CollectionStatus> lookupMap = new TreeMap<Integer, CollectionStatus>();

		private Holder() {
			// do nothing here
		}
	}

	private final int status;

	private CollectionStatus(int status) {
		this.status = status;
		Holder.lookupMap.put(status, this);
	}

	public int status() {
		return status;
	}

	public static CollectionStatus valueOf(int status) {
		return Holder.lookupMap.get(status);
	}
}
