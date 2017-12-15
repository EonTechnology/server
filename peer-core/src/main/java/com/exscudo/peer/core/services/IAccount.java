package com.exscudo.peer.core.services;

import java.util.Collection;

/**
 * The interface defines the basic properties of the account.
 * <p>
 * An account is treated as a set of properties that are changed through
 * transactions. The property specification is defined in the transaction type.
 *
 */
public interface IAccount {

	/**
	 * Returns the account identifier.
	 * 
	 * @return
	 */
	long getID();

	/**
	 * Returns the property defined by {@code id} or null.
	 * 
	 * @param id
	 *            One of the well-known types
	 * @return
	 */
	AccountProperty getProperty(String id);

	/**
	 * Adds a given {@code property}. If the property already exists, then its will
	 * be updated.
	 * 
	 * @param property
	 */
	void putProperty(AccountProperty property);

	/**
	 * Returns a list of all properties.
	 * 
	 * @return
	 */
	Collection<? extends AccountProperty> getProperties();

	/**
	 * Removes a property defined by {@code id}
	 * 
	 * @param id
	 */
	void removeProperty(String id);
}
