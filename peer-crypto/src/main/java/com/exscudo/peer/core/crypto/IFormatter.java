package com.exscudo.peer.core.crypto;

import java.util.Map;

public interface IFormatter {
    byte[] getBytes(Map<String, Object> map);
}
