package com.arangodb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.UserResult;
import com.arangodb.internal.ArangoDBConstants;
import com.arangodb.internal.CollectionCache;
import com.arangodb.internal.DocumentCache;
import com.arangodb.internal.net.Communication;
import com.arangodb.internal.velocypack.VPackConfigure;
import com.arangodb.model.ArangoDBImpl;
import com.arangodb.model.DB;
import com.arangodb.model.Executeable;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.velocypack.VPack;
import com.arangodb.velocypack.VPackDeserializer;
import com.arangodb.velocypack.VPackInstanceCreator;
import com.arangodb.velocypack.VPackParser;
import com.arangodb.velocypack.VPackSerializer;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public abstract class ArangoDB extends Executeable {

	public static class Builder {

		private static final String PROPERTY_KEY_HOST = "arangodb.host";
		private static final String PROPERTY_KEY_PORT = "arangodb.port";
		private static final String PROPERTY_KEY_TIMEOUT = "arangodb.timeout";
		private static final String DEFAULT_PROPERTY_FILE = "/arangodb.properties";

		private String host;
		private Integer port;
		private Integer timeout;
		private String user;
		private String password;
		private final VPack.Builder vpackBuilder;
		private final CollectionCache collectionCache;
		private final VPackParser vpackParser;

		public Builder() {
			super();
			vpackBuilder = new VPack.Builder();
			collectionCache = new CollectionCache();
			vpackParser = new VPackParser();
			VPackConfigure.configure(vpackBuilder, vpackParser, collectionCache);
			loadProperties(ArangoDB.class.getResourceAsStream(DEFAULT_PROPERTY_FILE));
		}

		public Builder loadProperties(final InputStream in) {
			if (in != null) {
				final Properties properties = new Properties();
				try {
					properties.load(in);
					host = getProperty(properties, PROPERTY_KEY_HOST, host, ArangoDBConstants.DEFAULT_HOST);
					port = Integer
							.parseInt(getProperty(properties, PROPERTY_KEY_PORT, port, ArangoDBConstants.DEFAULT_PORT));
					timeout = Integer.parseInt(
						getProperty(properties, PROPERTY_KEY_TIMEOUT, timeout, ArangoDBConstants.DEFAULT_TIMEOUT));
				} catch (final IOException e) {
					throw new ArangoDBException(e);
				}
			}
			return this;
		}

		private <T> String getProperty(
			final Properties properties,
			final String key,
			final T currentValue,
			final T defaultValue) {
			return properties.getProperty(key,
				currentValue != null ? currentValue.toString() : defaultValue.toString());
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

		public <T> Builder registerSerializer(final Class<T> clazz, final VPackSerializer<T> serializer) {
			vpackBuilder.registerSerializer(clazz, serializer);
			return this;
		}

		public <T> Builder registerDeserializer(final Class<T> clazz, final VPackDeserializer<T> deserializer) {
			vpackBuilder.registerDeserializer(clazz, deserializer);
			return this;
		}

		public <T> Builder regitserInstanceCreator(final Class<T> clazz, final VPackInstanceCreator<T> creator) {
			vpackBuilder.regitserInstanceCreator(clazz, creator);
			return this;
		}

		public ArangoDB build() {
			return new ArangoDBImpl(new Communication.Builder().host(host).port(port).timeout(timeout),
					vpackBuilder.build(), vpackBuilder.serializeNullValues(true).build(), vpackParser, collectionCache);
		}

	}

	protected ArangoDB(final Communication.Builder commBuilder, final VPack vpack, final VPack vpackNull,
		final VPackParser vpackParser, final CollectionCache collectionCache) {
		super(commBuilder.build(vpack, collectionCache), vpack, vpackNull, vpackParser, new DocumentCache(),
				collectionCache);
	}

	public abstract void shutdown();

	public abstract Boolean createDB(final String name) throws ArangoDBException;

	public abstract CompletableFuture<Boolean> createDBAsync(final String name);

	public abstract DB db();

	public abstract DB db(final String name);

	public abstract Collection<String> getDBs() throws ArangoDBException;

	public abstract CompletableFuture<Collection<String>> getDBsAsync();

	public abstract ArangoDBVersion getVersion() throws ArangoDBException;

	public abstract CompletableFuture<ArangoDBVersion> getVersionAsync();

	public abstract UserResult createUser(final String user, final String passwd, final UserCreateOptions options)
			throws ArangoDBException;

	public abstract CompletableFuture<UserResult> createUserAsync(
		final String user,
		final String passwd,
		final UserCreateOptions options);

	public abstract void deleteUser(final String user) throws ArangoDBException;

	public abstract CompletableFuture<Void> deleteUserAsync(final String user);

	public abstract UserResult getUser(String user);

	public abstract CompletableFuture<UserResult> getUserAsync(String user);

	public abstract Collection<UserResult> getUsers();

	public abstract CompletableFuture<Collection<UserResult>> getUsersAsync();

	public abstract UserResult updateUser(final String user, final UserUpdateOptions options);

	public abstract CompletableFuture<UserResult> updateUserAsync(String user, UserUpdateOptions options);
}
