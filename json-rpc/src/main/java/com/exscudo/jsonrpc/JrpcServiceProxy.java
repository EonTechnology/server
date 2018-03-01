package com.exscudo.jsonrpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Implementation of JSON-RPC client.
 * <p>
 * Used for requests to remote peers.
 */
public class JrpcServiceProxy {
    public static final String MESSAGE_ENCODING = "UTF-8";
    static final AtomicLong id = new AtomicLong();
    private final ObjectMapper objectMapper;

    private int connectTimeout = 1000;
    private int readTimeout = 1000;
    private URL url;

    public JrpcServiceProxy(URL endpoint) {
        this(endpoint, null);
    }

    public JrpcServiceProxy(URL endpoint, Module module) {
        this.url = endpoint;

        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        SimpleModule moduleRPC = new SimpleModule();
        moduleRPC.addSerializer(JsonRpcRequest.class, new JsonRpcRequestSerializer());
        objectMapper.registerModule(moduleRPC);

        if (module != null) {
            objectMapper.registerModule(module);
        }
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public long nextID() {
        return id.incrementAndGet();
    }

    public <TService> TService post(String method,
                                    Object[] params,
                                    Class<TService> clazz) throws IOException, JsonException {

        if (url == null) {
            throw new NullPointerException("url");
        }

        HttpURLConnection connection = null;
        String responseValue = null;

        long startTime = System.nanoTime();

        try {

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(getConnectTimeout());
            connection.setReadTimeout(getReadTimeout());
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("Content-Encoding", "gzip");

            JsonRpcRequest request = new JsonRpcRequest();
            request.setId(Long.toString(nextID()));
            request.setParams(params);
            request.setMethod(method);
            String requestStr = objectMapper.writeValueAsString(request);
            byte[] requestBytes = requestStr.getBytes(MESSAGE_ENCODING);

            OutputStream outputStream = new GZIPOutputStream(connection.getOutputStream());
            outputStream.write(requestBytes);
            outputStream.close();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String ceHeader = connection.getHeaderField("Content-Encoding");
                InputStream inputStream = ceHeader != null && ceHeader.toLowerCase().equals("gzip")
                                          ? new GZIPInputStream(connection.getInputStream())
                                          : connection.getInputStream();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[65536];
                int numberOfBytes;
                while ((numberOfBytes = inputStream.read(buffer)) > 0) {

                    byteArrayOutputStream.write(buffer, 0, numberOfBytes);
                }
                inputStream.close();
                responseValue = byteArrayOutputStream.toString(MESSAGE_ENCODING);

                Loggers.trace(JrpcServiceProxy.class,
                              "{}:{} >>> {} <<< {}",
                              url.getHost(),
                              url.getPort(),
                              requestStr,
                              responseValue);
            } else {
                // TODO: response code handlers
                throw new UnsupportedOperationException("Invalid response code '" +
                                                                connection.getResponseCode() +
                                                                "' received from server.");
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

        JsonNode errorNode = null;
        JsonNode resultNode = null;

        try {

            JsonNode rootNode = objectMapper.readTree(responseValue);
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                switch (field.getKey()) {
                    case "jsonrpc":
                        // version = field.getValue().asText();
                        break;
                    case "id":
                        // JsonNode value = field.getValue();
                        // if (!value.isNull()) {
                        // id = value.asText();
                        // }
                        break;
                    case "error":
                        errorNode = field.getValue();
                        break;
                    case "result":
                        resultNode = field.getValue();
                        break;
                    default:
                        break;
                }
            }

            if (errorNode != null) {
                int code = -1;
                String message = "";
                Iterator<Map.Entry<String, JsonNode>> iterator = errorNode.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> field = iterator.next();
                    switch (field.getKey()) {
                        case "code":
                            code = field.getValue().asInt();
                            break;
                        case "message":
                            message = field.getValue().asText();
                            break;
                        default:
                            break;
                    }
                }
                throw new JsonException("Unexcepted response. Error code: " + code + ". " + message);
            }

            if (clazz.equals(void.class)) {
                String retObj = objectMapper.treeToValue(resultNode, String.class);
                if (retObj.equals("success")) {
                    return null;
                } else {
                    throw new UnsupportedOperationException();
                }
            } else {
                TService retObj = objectMapper.treeToValue(resultNode, clazz);
                return retObj;
            }
        } catch (IOException e) {

            Loggers.trace(JrpcServiceProxy.class, "{} << Response: {}", url.getHost(), responseValue);
            throw new JsonException("Unable to parse response.");
        } finally {

            long timeRun = System.nanoTime() - startTime;
            Loggers.info(this.getClass(), "Timing:  {}ms - {} >> {}", timeRun / 1000000.0, method, url);
        }
    }

    static class JsonRpcRequest {

        /*
         * An identifier established by the Client that MUST contain a String, Number,
         * or NULL value if included. If it is not included it is assumed to be a
         * notification. The value SHOULD normally not be Null [1] and Numbers SHOULD
         * NOT contain fractional parts
         */
        private String id;

        /* A String containing the name of the method to be invoked. */
        private String method;

        /*
         * A Structured value that holds the parameter values to be used during the
         * invocation of the method.
         */
        private Object[] params;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Object[] getParams() {
            return params;
        }

        public void setParams(Object[] params) {
            this.params = params;
        }
    }

    static class JsonRpcRequestSerializer extends StdSerializer<JsonRpcRequest> {
        private static final long serialVersionUID = 1520274435229602322L;

        public JsonRpcRequestSerializer() {
            super(JsonRpcRequest.class);
        }

        @Override
        public void serialize(JsonRpcRequest value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            /*
             * A String specifying the version of the JSON-RPC protocol. MUST be exactly
             * "2.0".
             */
            gen.writeStringField("jsonrpc", "2.0");
            gen.writeStringField("method", value.getMethod());
            gen.writeStringField("id", value.getId());

            gen.writeArrayFieldStart("params");

            for (Object param : value.getParams()) {
                gen.writeObject(param);
            }
            gen.writeEndArray();

            gen.writeEndObject();
        }
    }
}
