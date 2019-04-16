package org.eontechnology.and.jsonrpc;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class DummyServiceTest {
    JrpcService baseService;

    private static String getResourceAsString(String path) throws IOException {
        InputStream inputStream = JrpcService.class.getResourceAsStream(path);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(JrpcServiceProxy.MESSAGE_ENCODING);
    }

    @Before
    public void setUp() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("class", new DummyServiceImpl());
        baseService = new JrpcService(new CompositeInnerService(map));
    }

    @Test
    public void parse_error() throws Exception {
        checkRequest("/org/eontechnology/and/jsonrpc/request_parse_error.json",
                     "/org/eontechnology/and/jsonrpc/response_parse_error.json");
    }

    @Test
    public void invalid_request() throws Exception {
        checkRequest("/org/eontechnology/and/jsonrpc/request_invalid_version.json",
                     "/org/eontechnology/and/jsonrpc/response_invalid_version.json");
    }

    @Test
    public void method_not_found() throws Exception {
        checkRequest("/org/eontechnology/and/jsonrpc/request_method_not_found.json",
                     "/org/eontechnology/and/jsonrpc/response_method_not_found.json");
    }

    @Test
    public void server_error() throws Exception {
        checkRequest("/org/eontechnology/and/jsonrpc/request_server_error.json",
                     "/org/eontechnology/and/jsonrpc/response_server_error.json");
    }

    @Test
    public void invalid_params() throws Exception {
        checkRequest("/org/eontechnology/and/jsonrpc/request_invalid_params.json",
                     "/org/eontechnology/and/jsonrpc/response_invalid_params.json");
    }

    @Test
    public void success() throws Exception {
        checkRequest("/org/eontechnology/and/jsonrpc/request_success.json",
                     "/org/eontechnology/and/jsonrpc/response_success.json");
    }

    @Test
    public void success_1() throws Exception {
        checkRequest("/org/eontechnology/and/jsonrpc/request_success_1.json",
                     "/org/eontechnology/and/jsonrpc/response_success_1.json");
    }

    @Test
    public void success_2() throws Exception {
        checkRequest("/org/eontechnology/and/jsonrpc/request_success_2.json",
                     "/org/eontechnology/and/jsonrpc/response_success_2.json");
    }

    private void checkRequest(String requestPath, String responsePath) throws Exception {

        String response = baseService.doRequest(getResourceAsString(requestPath));
        assertEquals(response, getResourceAsString(responsePath));
    }
}
