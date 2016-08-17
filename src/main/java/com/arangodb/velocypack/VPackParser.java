package com.arangodb.velocypack;

import java.io.IOException;
import java.util.Iterator;

import org.json.simple.JSONValue;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.arangodb.velocypack.exception.VPackBuilderException;
import com.arangodb.velocypack.exception.VPackBuilderNeedOpenCompoundException;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocypack.exception.VPackKeyTypeException;
import com.arangodb.velocypack.exception.VPackNeedAttributeTranslatorException;

/**
 * @author Mark - mark@arangodb.com
 *
 */
public class VPackParser {

	private static final char OBJECT_OPEN = '{';
	private static final char OBJECT_CLOSE = '}';
	private static final char ARRAY_OPEN = '[';
	private static final char ARRAY_CLOSE = ']';
	private static final char FIELD = ':';
	private static final char SEPARATOR = ',';
	private static final String NULL = "null";

	public static String toJson(final VPackSlice vpack) {
		return toJson(vpack, false);
	}

	public static String toJson(final VPackSlice vpack, final boolean includeNullValues) {
		final StringBuilder json = new StringBuilder();
		parse(null, vpack, json, includeNullValues);
		return json.toString();
	}

	private static void parse(
		final VPackSlice attribute,
		final VPackSlice value,
		final StringBuilder json,
		final boolean includeNullValues) {
		if (attribute != null) {
			appendField(attribute, json);
		}
		if (value.isObject()) {
			parseObject(value, json, includeNullValues);
		} else if (value.isArray()) {
			parseArray(value, json, includeNullValues);
		} else if (value.isBoolean()) {
			json.append(value.getAsBoolean());
		} else if (value.isString()) {
			json.append(JSONValue.toJSONString(value.getAsString()));
		} else if (value.isNumber()) {
			json.append(value.getAsNumber());
		} else if (value.isNull()) {
			json.append(NULL);
		}
	}

	private static void appendField(final VPackSlice attribute, final StringBuilder json) {
		try {
			json.append(JSONValue.toJSONString(attribute.makeKey().getAsString()));
		} catch (VPackKeyTypeException | VPackNeedAttributeTranslatorException e) {
			json.append(JSONValue.toJSONString(attribute.getAsString()));
		}
		json.append(FIELD);
	}

	private static void parseObject(final VPackSlice value, final StringBuilder json, final boolean includeNullValues) {
		json.append(OBJECT_OPEN);
		int added = 0;
		for (final Iterator<VPackSlice> iterator = value.iterator(); iterator.hasNext();) {
			final VPackSlice nextAttr = iterator.next();
			final VPackSlice nextValue = new VPackSlice(nextAttr.getVpack(),
					nextAttr.getStart() + nextAttr.getByteSize());
			if (!nextValue.isNull() || includeNullValues) {
				if (added++ > 0) {
					json.append(SEPARATOR);
				}
				parse(nextAttr, nextValue, json, includeNullValues);
			}
		}
		json.append(OBJECT_CLOSE);
	}

	private static void parseArray(final VPackSlice value, final StringBuilder json, final boolean includeNullValues) {
		json.append(ARRAY_OPEN);
		int added = 0;
		for (int i = 0; i < value.getLength(); i++) {
			final VPackSlice valueAt = value.get(i);
			if (!valueAt.isNull() || includeNullValues) {
				if (added++ > 0) {
					json.append(SEPARATOR);
				}
				parse(null, valueAt, json, includeNullValues);
			}
		}
		json.append(ARRAY_CLOSE);
	}

	public static VPackSlice fromJson(final String json) throws VPackException {
		return fromJson(json, false);
	}

	public static VPackSlice fromJson(final String json, final boolean includeNullValues) throws VPackException {
		final VPackBuilder builder = new VPackBuilder();
		final JSONParser parser = new JSONParser();
		final ContentHandler contentHandler = new VPackContentHandler(builder, includeNullValues);
		try {
			parser.parse(json, contentHandler);
		} catch (final ParseException e) {
			throw new VPackBuilderException(e);
		}
		return builder.slice();
	}

	private static class VPackContentHandler implements ContentHandler {

		private final VPackBuilder builder;
		private String attribute;
		private final boolean includeNullValues;

		public VPackContentHandler(final VPackBuilder builder, final boolean includeNullValues) {
			this.builder = builder;
			this.includeNullValues = includeNullValues;
			attribute = null;
		}

		private void add(final Value value) throws ParseException {
			try {
				builder.add(attribute, value);
				attribute = null;
			} catch (final VPackBuilderException e) {
				throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
			}
		}

		private void close() throws ParseException {
			try {
				builder.close();
			} catch (VPackBuilderNeedOpenCompoundException | VPackKeyTypeException
					| VPackNeedAttributeTranslatorException e) {
				throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
			}
		}

		@Override
		public void startJSON() throws ParseException, IOException {
		}

		@Override
		public void endJSON() throws ParseException, IOException {
		}

		@Override
		public boolean startObject() throws ParseException, IOException {
			add(new Value(ValueType.OBJECT));
			return true;
		}

		@Override
		public boolean endObject() throws ParseException, IOException {
			close();
			return true;
		}

		@Override
		public boolean startObjectEntry(final String key) throws ParseException, IOException {
			attribute = key;
			return true;
		}

		@Override
		public boolean endObjectEntry() throws ParseException, IOException {
			return true;
		}

		@Override
		public boolean startArray() throws ParseException, IOException {
			add(new Value(ValueType.ARRAY));
			return true;
		}

		@Override
		public boolean endArray() throws ParseException, IOException {
			close();
			return true;
		}

		@Override
		public boolean primitive(final Object value) throws ParseException, IOException {
			if (value == null) {
				if (includeNullValues) {
					add(new Value(ValueType.NULL));
				}
			} else if (String.class.isAssignableFrom(value.getClass())) {
				add(new Value(String.class.cast(value)));
			} else if (Boolean.class.isAssignableFrom(value.getClass())) {
				add(new Value(Boolean.class.cast(value)));
			} else if (Double.class.isAssignableFrom(value.getClass())) {
				add(new Value(Double.class.cast(value)));
			} else if (Number.class.isAssignableFrom(value.getClass())) {
				add(new Value(Long.class.cast(value)));
			}
			return true;
		}

	}

}