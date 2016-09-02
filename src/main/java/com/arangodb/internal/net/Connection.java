package com.arangodb.internal.net;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Optional;

import javax.net.SocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoDBException;
import com.arangodb.internal.ArangoDBConstants;
import com.arangodb.internal.net.velocystream.Chunk;
import com.arangodb.internal.net.velocystream.Message;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.internal.util.NumberUtil;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public abstract class Connection {

	private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

	protected Optional<String> host = Optional.empty();
	protected Optional<Integer> port = Optional.empty();
	protected Optional<Integer> timeout = Optional.empty();

	protected Socket socket;
	protected OutputStream outputStream;
	protected InputStream inputStream;

	protected Connection(final String host, final Integer port, final Integer timeout) {
		super();
		this.host = Optional.of(host);
		this.port = Optional.of(port);
		this.timeout = Optional.ofNullable(timeout);
	}

	public synchronized boolean isOpen() {
		return socket != null && socket.isConnected() && !socket.isClosed();
	}

	public synchronized void open() throws IOException {
		if (isOpen()) {
			return;
		}
		socket = SocketFactory.getDefault().createSocket();
		final String host = this.host.orElse(ArangoDBConstants.DEFAULT_HOST);
		final Integer port = this.port.orElse(ArangoDBConstants.DEFAULT_PORT);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Open connection to addr=%s,port=%s", host, port));
		}
		socket.connect(new InetSocketAddress(host, port), timeout.orElse(ArangoDBConstants.DEFAULT_TIMEOUT));
		socket.setKeepAlive(true);
		socket.setTcpNoDelay(true);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Connected to %s", socket));
		}

		outputStream = new BufferedOutputStream(socket.getOutputStream());
		inputStream = socket.getInputStream();
	}

	public synchronized void close() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Close connection %s", socket));
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (final IOException e) {
				throw new ArangoDBException(e);
			}
		}
	}

	protected synchronized void writeIntern(final Message message, final Collection<Chunk> chunks) {
		chunks.stream().forEach(chunk -> {
			try {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Send chunk %s:%s from message %s", chunk.getChunk(),
						chunk.isFirstChunk() ? 1 : 0, chunk.getMessageId()));
				}
				writeChunkHead(chunk);
				final int contentOffset = chunk.getContentOffset();
				final int contentLength = chunk.getContentLength();
				final VPackSlice head = message.getHead();
				final int headLength = head.getByteSize();
				int written = 0;
				if (contentOffset < headLength) {
					written = Math.min(contentLength, headLength - contentOffset);
					outputStream.write(head.getVpack(), contentOffset, written);
				}
				if (written < contentLength) {
					final VPackSlice body = message.getBody().get();
					outputStream.write(body.getVpack(), contentOffset + written - headLength, contentLength - written);
				}
				outputStream.flush();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void writeChunkHead(final Chunk chunk) throws IOException {
		final long messageLength = chunk.getMessageLength();
		final int headLength = messageLength > -1L ? ArangoDBConstants.CHUNK_MAX_HEADER_SIZE
				: ArangoDBConstants.CHUNK_MIN_HEADER_SIZE;
		final int length = chunk.getContentLength() + headLength;
		final ByteBuffer buffer = ByteBuffer.allocate(headLength).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(length);
		buffer.putInt(chunk.getChunkX());
		buffer.putLong(chunk.getMessageId());
		if (messageLength > -1L) {
			buffer.putLong(messageLength);
		}
		outputStream.write(buffer.array());
	}

	protected Chunk read() throws IOException, BufferUnderflowException {
		final Chunk chunk = readChunkHead();
		final byte[] content = new byte[chunk.getContentLength()];
		// chunk.setContent(content);
		// TODO
		readBytes(content.length).get(content);
		return chunk;
	}

	protected Chunk readChunkHead() throws IOException {
		final int length = readInt();
		final int chunkX = readInt();
		final long messageId = readLong();
		final long messageLength;
		final int contentLength;
		if ((1 == (chunkX & 0x1)) && ((chunkX >> 1) > 1)) {
			messageLength = readLong();
			contentLength = length - ArangoDBConstants.CHUNK_MAX_HEADER_SIZE;
		} else {
			messageLength = -1L;
			contentLength = length - ArangoDBConstants.CHUNK_MIN_HEADER_SIZE;
		}
		final Chunk chunk = new Chunk(messageId, chunkX, messageLength, 0, contentLength);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Received chunk %s:%s from message %s", chunk.getChunk(),
				chunk.isFirstChunk() ? 1 : 0, chunk.getMessageId()));
		}
		return chunk;
	}

	private int readInt() throws IOException {
		final byte[] buf = new byte[Integer.BYTES];
		readBytesIntoBuffer(buf, 0, buf.length);
		return (int) NumberUtil.toLong(buf, 0, buf.length);
	}

	private long readLong() throws IOException {
		final byte[] buf = new byte[Long.BYTES];
		readBytesIntoBuffer(buf, 0, buf.length);
		return NumberUtil.toLong(buf, 0, buf.length);
	}

	private ByteBuffer readBytes(final int len) throws IOException {
		final byte[] buf = new byte[len];
		readBytesIntoBuffer(buf, 0, len);
		return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
	}

	protected void readBytesIntoBuffer(final byte[] buf, final int off, final int len) throws IOException {
		for (int readed = 0; readed < len;) {
			final int read = inputStream.read(buf, off + readed, len - readed);
			if (read == -1) {
				throw new IOException("Reached the end of the stream.");
			} else {
				readed += read;
			}
		}
	}

}
