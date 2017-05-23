package com.exscudo.eon;

/**
 * Represents a provider of value.
 *
 * @param <T>
 *            the type of value provided by this provider
 */
public interface Provider<T> {

	/**
	 * Gets a value.
	 *
	 * @return a value
	 */
	T get();
}
