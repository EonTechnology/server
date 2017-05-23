package com.exscudo.eon.peer;

import com.exscudo.eon.exceptions.DecodeException;

public class HallmarkProcessor {

	public static boolean analyze(Peer peer, Hallmark hmark, ExecutionContext context) throws DecodeException {

		if (!hmark.getHost().equals(peer.getAnnouncedAddress())) {

			throw new DecodeException(
					"Invalid host name. Source: " + peer.getAnnouncedAddress() + ", announced" + hmark.getHost());

		}

		// TODO: not yet implemented

		return false;

	}
}
