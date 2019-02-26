package org.eontechology.and.jsonrpc;

class DummyServiceImpl {

    public boolean method(int arg0, String arg1) {
        return true;
    }

    public void method1(int arg0, String arg1) throws IllegalArgumentException {
        throw new IllegalArgumentException("args1");
    }

    public void method2(int arg0, String arg1) throws Exception {
        throw new Exception("some exception.");
    }
}
