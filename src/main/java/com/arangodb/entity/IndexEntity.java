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

package com.arangodb.entity;

import java.util.Collection;
import java.util.Optional;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class IndexEntity {

	private String id;
	private IndexType type;
	private Collection<String> fields;
	private Integer selectivityEstimate;
	private Boolean unique;
	private Boolean sparse;
	private Integer minLength;
	private Boolean isNewlyCreated;
	private Boolean geoJson;
	private Boolean constraint;

	public IndexEntity() {
		super();
	}

	public String getId() {
		return id;
	}

	public IndexType getType() {
		return type;
	}

	public Collection<String> getFields() {
		return fields;
	}

	public Optional<Integer> getSelectivityEstimate() {
		return Optional.ofNullable(selectivityEstimate);
	}

	public Optional<Boolean> getUnique() {
		return Optional.ofNullable(unique);
	}

	public Optional<Boolean> getSparse() {
		return Optional.ofNullable(sparse);
	}

	public Optional<Integer> getMinLength() {
		return Optional.ofNullable(minLength);
	}

	public Optional<Boolean> getIsNewlyCreated() {
		return Optional.ofNullable(isNewlyCreated);
	}

	public Optional<Boolean> getGeoJson() {
		return Optional.ofNullable(geoJson);
	}

	public Optional<Boolean> getConstraint() {
		return Optional.ofNullable(constraint);
	}

}
