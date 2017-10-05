package com.exscudo.eon.jsonrpc;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.exscudo.eon.jsonrpc.serialization.StringConstant;

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
		return result.toString(StringConstant.messageEncoding);
	}

	@Before
	public void setUp() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("class", new DummyServiceImpl());
		baseService = new JrpcService(map);
	}

	@Test
	public void parse_error() throws Exception {
		checkRequest("/com/exscudo/eon/request_parse_error.json", "/com/exscudo/eon/parse_error.json");
	}

	@Test
	public void invalid_request() throws Exception {
		checkRequest("/com/exscudo/eon/request_invalid.json", "/com/exscudo/eon/invalid_request.json");
	}

	@Test
	public void method_not_found() throws Exception {
		checkRequest("/com/exscudo/eon/request.json", "/com/exscudo/eon/method_not_found.json");
	}

	@Test
	public void server_error() throws Exception {
		checkRequest("/com/exscudo/eon/request_method2.json", "/com/exscudo/eon/server_error.json");
	}

	@Test
	public void invalid_params() throws Exception {
		checkRequest("/com/exscudo/eon/request_method1.json", "/com/exscudo/eon/invalid_params.json");
	}

	@Test
	public void success() throws Exception {
		checkRequest("/com/exscudo/eon/request_method.json", "/com/exscudo/eon/success.json");
	}

	private void checkRequest(String requestPath, String responsePath) throws Exception {

		String response = baseService.doRequest(getResourceAsString(requestPath));
		assertEquals(response, getResourceAsString(responsePath));
	}

}
