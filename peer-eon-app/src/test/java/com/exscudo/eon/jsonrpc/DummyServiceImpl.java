package com.exscudo.eon.jsonrpc;

import com.exscudo.peer.core.exceptions.RemotePeerException;

class DummyServiceImpl {

	public boolean method(int arg0, String arg1) {
		return true;
	}

	public void method1(int arg0, String arg1) throws RemotePeerException {
		throw new RemotePeerException("args1");
	}

	public void method2(int arg0, String arg1) throws Exception {
		throw new Exception("some exception.");
	}
}
