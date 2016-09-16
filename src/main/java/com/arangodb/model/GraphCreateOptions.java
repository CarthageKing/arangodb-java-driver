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

import java.util.Arrays;
import java.util.Collection;

import com.arangodb.entity.EdgeDefinition;

/**
 * @author Mark - mark at arangodb.com
 * 
 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#create-a-graph">API Documentation</a>
 */
public class GraphCreateOptions {

	private String name;
	private Collection<EdgeDefinition> edgeDefinitions;
	private Collection<String> orphanCollections;

	public GraphCreateOptions() {
		super();
	}

	protected String getName() {
		return name;
	}

	protected GraphCreateOptions name(final String name) {
		this.name = name;
		return this;
	}

	public Collection<EdgeDefinition> getEdgeDefinitions() {
		return edgeDefinitions;
	}

	protected GraphCreateOptions edgeDefinitions(final Collection<EdgeDefinition> edgeDefinitions) {
		this.edgeDefinitions = edgeDefinitions;
		return this;
	}

	public Collection<String> getOrphanCollections() {
		return orphanCollections;
	}

	/**
	 * @param orphanCollections
	 *            Additional vertex collections
	 * @return options
	 */
	public GraphCreateOptions orphanCollections(final String... orphanCollections) {
		this.orphanCollections = Arrays.asList(orphanCollections);
		return this;
	}

}
