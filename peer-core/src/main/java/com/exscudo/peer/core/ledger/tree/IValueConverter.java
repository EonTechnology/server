package com.exscudo.peer.core.ledger.tree;

import java.util.Map;

public interface IValueConverter<T> {

    /**
     * @return
     */
    T convert(Map<String, Object> map);

    /**
     * @param value
     * @return
     */
    Map<String, Object> convert(T value);
}
