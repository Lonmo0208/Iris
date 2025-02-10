package net.irisshaders.iris.helpers;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An absurdly simple class for storing pairs of strings because Java lacks pair / tuple types.
 */
public class StringPair {
	private final String key;
	private final String value;

	public StringPair(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() { return key; }
	public String getValue() { return value; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StringPair that = (StringPair) o;
		return key.equals(that.key) && value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@NotNull
	public String key() {
		return key;
	}

	@NotNull
	public String value() {
		return value;
	}


}
