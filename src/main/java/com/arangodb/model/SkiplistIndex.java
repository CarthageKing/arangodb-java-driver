package com.arangodb.model;

import java.util.Collection;

import com.arangodb.entity.IndexType;

/**
 * @author Mark - mark at arangodb.com
 *
 */
@SuppressWarnings("unused")
public class SkiplistIndex {

	private final Collection<String> fields;
	private final IndexType type;
	private final Boolean unique;
	private final Boolean sparse;

	private SkiplistIndex(final Collection<String> fields, final IndexType type, final Boolean unique,
		final Boolean sparse) {
		super();
		this.fields = fields;
		this.type = type;
		this.unique = unique;
		this.sparse = sparse;
	}

	public static class Options {

		private Boolean unique;
		private Boolean sparse;

		public Options unique(final Boolean unique) {
			this.unique = unique;
			return this;
		}

		public Options sparse(final Boolean sparse) {
			this.sparse = sparse;
			return this;
		}

		public SkiplistIndex build(final Collection<String> fields) {
			return new SkiplistIndex(fields, IndexType.skiplist, unique, sparse);
		}
	}
}
