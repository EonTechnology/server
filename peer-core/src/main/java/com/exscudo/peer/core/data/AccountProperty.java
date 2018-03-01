package com.exscudo.peer.core.data;

import java.io.Serializable;
import java.util.Map;

/**
 * Account property.
 * <p>
 * It is changed by transaction. The specificity of a property depends on the
 * interpretation of a particular type of transaction
 */
public class AccountProperty implements Serializable {
    private static final long serialVersionUID = 2326856545894572608L;

    private String type;
    private Map<String, Object> data;

    public AccountProperty(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }

    public Map<String, Object> getData() {
        if (data == null) {
            throw new RuntimeException();
        }
        return data;
    }

    public String getType() {
        return type;
    }
}
