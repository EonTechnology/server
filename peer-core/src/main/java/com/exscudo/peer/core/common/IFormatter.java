package com.exscudo.peer.core.common;

import java.util.Map;

public interface IFormatter {
    byte[] getBytes(Map<String, Object> map);
}
