package com.exscudo.peer.eon.utils;

import com.exscudo.peer.core.utils.Format;

public class ColoredCoinId {
	private static final String COLORED_COIN_PREFIX = "EON-C";

	public static long convert(String id) throws IllegalArgumentException {
		return Format.UserFriendlyID.Decode(id, COLORED_COIN_PREFIX);
	}

	public static String convert(long id) {
		return Format.UserFriendlyID.Encode(id, COLORED_COIN_PREFIX);
	}
}
