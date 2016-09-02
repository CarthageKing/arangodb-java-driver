package com.arangodb;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import com.arangodb.entity.CursorResult;
import com.arangodb.entity.CursorResult.Extras;
import com.arangodb.entity.CursorResult.Stats;
import com.arangodb.entity.CursorResult.Warning;
import com.arangodb.internal.ArangoDBConstants;
import com.arangodb.internal.net.Request;
import com.arangodb.internal.net.velocystream.RequestType;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoCursor<T> implements Iterable<T>, Closeable {

	private final ArangoDatabase db;
	private final Class<T> type;
	private final ArangoCursorIterator<T> iterator;
	private final String id;
	private final Optional<Integer> count;
	private final Optional<Extras> extra;
	private final boolean cached;

	public ArangoCursor(final ArangoDatabase db, final Class<T> type, final CursorResult result) {
		super();
		this.db = db;
		this.type = type;
		count = Optional.ofNullable(result.getCount());
		extra = Optional.ofNullable(result.getExtra());
		cached = result.getCached().booleanValue();
		iterator = new ArangoCursorIterator<>(this, result);
		id = result.getId();
	}

	protected ArangoDatabase getDb() {
		return db;
	}

	public Class<T> getType() {
		return type;
	}

	public Optional<Integer> getCount() {
		return count;
	}

	public Optional<Stats> getStats() {
		return extra.map(e -> e.getStats());
	}

	public Optional<Collection<Warning>> getWarnings() {
		return extra.map(e -> e.getWarnings());
	}

	public boolean isCached() {
		return cached;
	}

	protected CursorResult executeNext() {
		return db.executeSync(new Request(db.name(), RequestType.PUT, db.createPath(ArangoDBConstants.PATH_API_CURSOR, id)),
			CursorResult.class);
	}

	@Override
	public Iterator<T> iterator() {
		return iterator;
	}

	@Override
	public void close() throws IOException {
		db.executeSync(new Request(db.name(), RequestType.DELETE, db.createPath(ArangoDBConstants.PATH_API_CURSOR, id)),
			Void.class);
	}

}
