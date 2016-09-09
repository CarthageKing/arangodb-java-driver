package com.arangodb.internal.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoDBException;
import com.arangodb.entity.ErrorEntity;
import com.arangodb.internal.ArangoDBConstants;
import com.arangodb.internal.CollectionCache;
import com.arangodb.internal.net.velocystream.Chunk;
import com.arangodb.internal.net.velocystream.Message;
import com.arangodb.internal.net.velocystream.MessageStore;
import com.arangodb.velocypack.VPack;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocypack.exception.VPackParserException;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class Communication {

	private static final int SPARTA = 300;

	private static final Logger LOGGER = LoggerFactory.getLogger(Communication.class);

	private static final AtomicLong mId = new AtomicLong(0L);
	private final VPack vpack;
	private final ConnectionSync connectionSync;
	private final ConnectionAsync connectionAsync;
	private final MessageStore messageStore;
	private final CollectionCache collectionCache;

	private final Optional<String> user;
	private final Optional<String> password;

	public static class Builder {
		private String host;
		private Integer port;
		private Integer timeout;
		private String user;
		private String password;

		public Builder() {
			super();
		}

		public Builder host(final String host) {
			this.host = host;
			return this;
		}

		public Builder port(final int port) {
			this.port = port;
			return this;
		}

		public Builder timeout(final Integer timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder user(final String user) {
			this.user = user;
			return this;
		}

		public Builder password(final String password) {
			this.password = password;
			return this;
		}

		public Communication build(final VPack vpack, final CollectionCache collectionCache) {
			return new Communication(host, port, timeout, user, password, vpack, collectionCache);
		}
	}

	private Communication(final String host, final Integer port, final Integer timeout, final String user,
		final String password, final VPack vpack, final CollectionCache collectionCache) {
		messageStore = new MessageStore();
		this.user = Optional.ofNullable(user);
		this.password = Optional.ofNullable(password);
		this.vpack = vpack;
		this.collectionCache = collectionCache;
		connectionAsync = new ConnectionAsync.Builder(messageStore).host(host).port(port).timeout(timeout).build();
		connectionSync = new ConnectionSync.Builder().host(host).port(port).timeout(timeout).build();
	}

	private void connect(final Connection connection, final boolean sync) {
		if (!connection.isOpen()) {
			try {
				connection.open();
				user.ifPresent(u -> authenticate(sync));
			} catch (final IOException e) {
				LOGGER.error(e.getMessage(), e);
				throw new ArangoDBException(e);
			}
		}
	}

	private void authenticate(final boolean sync) {
		Response response = null;
		if (sync) {
			response = executeSync(
				new AuthenticationRequest(user.get(), password.orElse(""), ArangoDBConstants.ENCRYPTION_PLAIN));
		} else {
			try {
				response = executeAsync(
					new AuthenticationRequest(user.get(), password.orElse(""), ArangoDBConstants.ENCRYPTION_PLAIN))
							.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new ArangoDBException(e);
			}
		}
		checkError(response);
	}

	public void disconnect() {
		disconnect(connectionAsync);
		disconnect(connectionSync);
	}

	public void disconnect(final Connection connection) {
		connection.close();
	}

	public Response executeSync(final Request request) throws ArangoDBException {
		connect(connectionSync, true);
		try {
			final Message requestMessage = createMessage(request);
			final Message responseMessage = sendSync(requestMessage);
			collectionCache.setDb(request.getDatabase());
			final Response response = createResponse(responseMessage);
			checkError(response);
			return response;
		} catch (VPackParserException | IOException e) {
			throw new ArangoDBException(e);
		}

	}

	private void checkError(final Response response) throws ArangoDBException {
		try {
			if (response.getResponseCode() >= SPARTA) {
				if (response.getBody().isPresent()) {
					throw new ArangoDBException(createErrorMessage(response));
				} else {
					throw new ArangoDBException(String.format("Response Code: %s", response.getResponseCode()));
				}
			}
		} catch (final VPackParserException e) {
			throw new ArangoDBException(e);
		}
	}

	private String createErrorMessage(final Response response) throws VPackParserException {
		String errorMessage;
		final ErrorEntity errorEntity = vpack.deserialize(response.getBody().get(), ErrorEntity.class);
		errorMessage = String.format("Response: %s, Error: %s - %s", errorEntity.getCode(), errorEntity.getErrorNum(),
			errorEntity.getErrorMessage());
		return errorMessage;
	}

	public CompletableFuture<Response> executeAsync(final Request request) {
		connect(connectionAsync, false);
		final CompletableFuture<Response> rfuture = new CompletableFuture<>();
		try {
			final Message message = createMessage(request);
			sendAsync(message).whenComplete((m, ex) -> {
				if (m != null) {
					try {
						collectionCache.setDb(request.getDatabase());
						final Response response = createResponse(m);
						if (response.getResponseCode() >= 300) {
							if (response.getBody().isPresent()) {
								final ErrorEntity errorEntity = vpack.deserialize(response.getBody().get(),
									ErrorEntity.class);
								final String errorMessage = String.format("Response: %s, Error: %s - %s",
									errorEntity.getCode(), errorEntity.getErrorNum(), errorEntity.getErrorMessage());
								rfuture.completeExceptionally(new ArangoDBException(errorMessage));
							} else {
								rfuture.completeExceptionally(new ArangoDBException(
										String.format("Response Code: %s", response.getResponseCode())));
							}
						} else {
							rfuture.complete(response);
						}
					} catch (final VPackParserException e) {
						LOGGER.error(e.getMessage(), e);
						rfuture.completeExceptionally(e);
					}
				} else if (ex != null) {
					LOGGER.error(ex.getMessage(), ex);
					rfuture.completeExceptionally(ex);
				} else {
					rfuture.cancel(true);
				}
			});
		} catch (final IOException | VPackException e) {
			LOGGER.error(e.getMessage(), e);
			rfuture.completeExceptionally(e);
		}
		return rfuture;
	}

	private Response createResponse(final Message messsage) throws VPackParserException {
		final Response response = vpack.deserialize(messsage.getHead(), Response.class);
		if (messsage.getBody().isPresent()) {
			response.setBody(messsage.getBody().get());
		}
		return response;
	}

	private Message createMessage(final Request request) throws VPackParserException {
		final long id = mId.incrementAndGet();
		final VPackSlice body = request.getBody().isPresent() ? request.getBody().get() : null;
		return new Message(id, vpack.serialize(request), body);
	}

	private CompletableFuture<Message> sendAsync(final Message message) throws IOException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Send Message (id=%s, head=%s, body=%s)", message.getId(), message.getHead(),
				message.getBody().isPresent() ? message.getBody().get() : "{}"));
		}
		return connectionAsync.write(message, buildChunks(message));
	}

	private Message sendSync(final Message message) throws IOException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Send Message (id=%s, head=%s, body=%s)", message.getId(), message.getHead(),
				message.getBody().isPresent() ? message.getBody().get() : "{}"));
		}
		return connectionSync.write(message, buildChunks(message));
	}

	private Collection<Chunk> buildChunks(final Message message) throws IOException {
		final Collection<Chunk> chunks = new ArrayList<>();
		final VPackSlice head = message.getHead();
		int size = head.getByteSize();
		final Optional<VPackSlice> body = message.getBody();
		if (body.isPresent()) {
			size += body.get().getByteSize();
		}
		final int n = size / ArangoDBConstants.CHUNK_BODY_SIZE;
		final int numberOfChunks = (size % ArangoDBConstants.CHUNK_BODY_SIZE != 0) ? (n + 1) : n;
		int off = 0;
		for (int i = 0; size > 0; i++) {
			final int len = Math.min(ArangoDBConstants.CHUNK_BODY_SIZE, size);
			final long messageLength = (i == 0 && numberOfChunks > 1) ? size : -1L;
			final Chunk chunk = new Chunk(message.getId(), i, numberOfChunks, messageLength, off, len);
			size -= len;
			off += len;
			chunks.add(chunk);
		}
		return chunks;
	}

}
