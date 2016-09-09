package com.arangodb.entity;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.arangodb.internal.velocypack.VPackConfigure;
import com.arangodb.velocypack.VPack;
import com.arangodb.velocypack.VPack.Builder;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackParser;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.ValueType;
import com.arangodb.velocypack.exception.VPackException;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class BaseDocumentTest {

	@Test
	public void serialize() throws VPackException {
		final BaseDocument entity = new BaseDocument();
		entity.setId("test/test");
		entity.setKey("test");
		entity.setRevision("test");
		entity.addAttribute("_id", "test");
		entity.addAttribute("a", "a");

		final Builder builder = new VPack.Builder();
		VPackConfigure.configure(builder, new VPackParser(), null);
		final VPack vpacker = builder.build();

		final VPackSlice vpack = vpacker.serialize(entity);
		assertThat(vpack, is(notNullValue()));
		assertThat(vpack.isObject(), is(true));
		assertThat(vpack.size(), is(4));

		final VPackSlice id = vpack.get("_id");
		assertThat(id.isString(), is(true));
		assertThat(id.getAsString(), is("test/test"));

		final VPackSlice key = vpack.get("_key");
		assertThat(key.isString(), is(true));
		assertThat(key.getAsString(), is("test"));

		final VPackSlice rev = vpack.get("_rev");
		assertThat(rev.isString(), is(true));
		assertThat(rev.getAsString(), is("test"));

		final VPackSlice a = vpack.get("a");
		assertThat(a.isString(), is(true));
		assertThat(a.getAsString(), is("a"));
	}

	@Test
	public void deserialize() throws VPackException {
		final VPackBuilder builder = new VPackBuilder();
		builder.add(ValueType.OBJECT);
		builder.add("_id", "test/test");
		builder.add("_key", "test");
		builder.add("_rev", "test");
		builder.add("a", "a");
		builder.close();

		final VPack.Builder vbuilder = new VPack.Builder();
		VPackConfigure.configure(vbuilder, new VPackParser(), null);
		final VPack vpacker = vbuilder.build();

		final BaseDocument entity = vpacker.deserialize(builder.slice(), BaseDocument.class);
		assertThat(entity.getId(), is(notNullValue()));
		assertThat(entity.getId(), is("test/test"));
		assertThat(entity.getKey(), is(notNullValue()));
		assertThat(entity.getKey(), is("test"));
		assertThat(entity.getRevision(), is(notNullValue()));
		assertThat(entity.getRevision(), is("test"));
		assertThat(entity.getProperties().size(), is(1));
		assertThat(entity.getAttribute("a"), is("a"));
	}

}
