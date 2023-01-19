package org.eontechnology.and.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

/**
 * Implementation of JSON-RPC service.
 *
 * <p>Used for handling incoming requests.
 */
public class JrpcService {

  private final ObjectMapper objectMapper;
  private final IInnerService innerService;

  public JrpcService(IInnerService innerService) {
    this(innerService, null);
  }

  public JrpcService(IInnerService innerService, Module module) {
    this.innerService = innerService;

    objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    SimpleModule moduleRPC = new SimpleModule();
    moduleRPC.addSerializer(JsonRpcResponse.class, new JsonRpcResponseSerializer());
    objectMapper.registerModule(moduleRPC);

    if (module != null) {
      objectMapper.registerModule(module);
    }
  }

  public String doRequest(String requestBody) throws IOException {

    Class logFromClazz = JrpcService.class;

    JsonRpcResponse response = new JsonRpcResponse();

    String version = null;
    JsonNode id = null;
    String methodName = null;
    JsonNode paramsNode = null;

    long startTime = System.nanoTime();

    try {

      JsonNode rootNode = objectMapper.readTree(requestBody);
      Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
      while (fieldsIterator.hasNext()) {
        Map.Entry<String, JsonNode> field = fieldsIterator.next();

        switch (field.getKey()) {
          case "jsonrpc":
            /*
             * A String specifying the version of the JSON-RPC protocol. MUST be exactly
             * "2.0".
             */
            version = field.getValue().asText();
            break;
          case "id":
            /*
             * An identifier established by the Client that MUST contain a String, Number,
             * or NULL value if included
             */
            id = field.getValue();
            break;
          case "method":
            /*
             * A String containing the name of the method to be invoked.
             */
            methodName = field.getValue().asText();
            break;
          case "params":
            /*
             *
             * If present, parameters for the rpc call MUST be provided as a Structured
             * value. Either by-position through an Array or by-name through an Object.
             *
             * by-position: params MUST be an Array, containing the values in the Server
             * expected order. by-name: params MUST be an Object, with member names that
             * match the Server expected parameter names. The absence of expected names MAY
             * result in an error being generated. The names MUST match exactly, including
             * case, to the method's expected parameters.
             */
            paramsNode = field.getValue();
            break;
          default:
            break;
        }
      }

      if (String.valueOf(version).equals("2.0")) {

        InnerServiceMethod method = innerService.getMethod(methodName);

        if (method != null) {
          logFromClazz = method.getServiceClass();

          try {

            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            if (parameterTypes.length > 0) {

              if (paramsNode.isArray()) {
                int i = 0;
                for (JsonNode itemNode : paramsNode) {
                  Object obj = objectMapper.treeToValue(itemNode, parameterTypes[i]);
                  args[i] = obj;
                  if (++i > args.length) {
                    break;
                  }
                }
              } else {
                Object obj = objectMapper.treeToValue(paramsNode, parameterTypes[0]);
                args[0] = obj;
              }
            }

            Object retObj = method.invoke(args);
            if (method.getReturnType().equals(void.class)) {
              response.setResult();
            } else {
              response.setResult(retObj);
            }
            response.setId(id);
          } catch (InvocationTargetException e) {

            Throwable target = e.getTargetException();
            if (target instanceof IllegalArgumentException) {
              if (target.getCause() != null) {
                response.setError(
                    new JsonRpcResponse.Error(
                        JsonRpcResponse.INVALID_PARAMS, target.getCause().getMessage()));
              } else {
                response.setError(
                    new JsonRpcResponse.Error(JsonRpcResponse.INVALID_PARAMS, target.getMessage()));
              }
            } else {
              response.setError(
                  new JsonRpcResponse.Error(JsonRpcResponse.SERVER_ERROR, target.getMessage()));
            }
          } catch (Exception e) {
            response.setError(
                new JsonRpcResponse.Error(JsonRpcResponse.SERVER_ERROR, e.getMessage()));
          }
        } else {
          response.setError(
              new JsonRpcResponse.Error(
                  JsonRpcResponse.METHOD_NOT_FOUND,
                  "The method '" + methodName + "' does not exist."));
        }
      } else {
        response.setError(
            new JsonRpcResponse.Error(JsonRpcResponse.INVALID_REQUEST, "Invalid request."));
      }
    } catch (IOException e) {
      response.setError(
          new JsonRpcResponse.Error(
              JsonRpcResponse.PARSE_ERROR, "An error occurred while parsing the JSON text."));
    }

    String responseBody = objectMapper.writeValueAsString(response);

    boolean showLog = true;
    if (showLog) {

      long timeRun = System.nanoTime() - startTime;

      String remotePeer = RequestContextHolder.getRemoteHost();

      Loggers.debug(
          logFromClazz, "Timing:  {}ms - {} >> {}", timeRun / 1000000.0, methodName, remotePeer);

      Loggers.trace(
          logFromClazz,
          "{} >> Request: {}; Response: {}",
          remotePeer == null ? "" : remotePeer,
          requestBody != null ? requestBody : "NULL",
          responseBody);
    }
    return responseBody;
  }

  private static class JsonRpcResponse {

    /* An error occurred on the server while parsing the JSON text. */
    static final int PARSE_ERROR = -32700;
    /* The JSON sent is not a valid Request object. */
    static final int INVALID_REQUEST = -32600;
    /* The method does not exist / is not available. */
    static final int METHOD_NOT_FOUND = -32601;
    /* Invalid method parameter(s). */
    static final int INVALID_PARAMS = -32602;
    /* Internal JSON-RPC error */
    static final int INTERNAL_ERROR = -32603;
    /* Server error */
    static final int SERVER_ERROR = -32099;
    /*
     * A String specifying the version of the JSON-RPC protocol. MUST be exactly
     * "2.0".
     */
    private String jsonrpc = "2.0";
    /*
     * This member is REQUIRED. It MUST be the same as the value of the id member in
     * the Request Object. If there was an error in detecting the id in the Request
     * object (e.g. Parse error/Invalid Request), it MUST be Null.
     */
    private JsonNode id;
    /*
     * This member is REQUIRED on success. This member MUST NOT exist if there was
     * an error invoking the method.
     */
    private Object result;
    /*
     * This member is REQUIRED on success. This member MUST NOT exist if there was
     * an error invoking the method.
     */
    private Error error;

    public String getJsonrpc() {
      return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
      this.jsonrpc = jsonrpc;
    }

    public JsonNode getId() {
      return id;
    }

    public void setId(JsonNode id) {
      this.id = id;
    }

    public Object getResult() {
      return result;
    }

    public void setResult(Object result) {
      this.result = result;
    }

    public void setResult() {
      this.result = "success";
    }

    public Error getError() {
      return error;
    }

    public void setError(Error error) {
      this.error = error;
    }

    static class Error {
      /*
       * A Number that indicates the error type that occurred. This MUST be an
       * integer.
       */
      private int code;

      /*
       * A String providing a short description of the error. The message SHOULD be
       * limited to a concise single sentence.
       */
      private String message;

      /*
       * A Primitive or Structured value that contains additional information about
       * the error
       */
      private String data;

      public Error(int code, String message) {
        this.setCode(code);
        this.setMessage(message);
      }

      public int getCode() {
        return code;
      }

      public void setCode(int code) {
        this.code = code;
      }

      public String getMessage() {
        return message;
      }

      public void setMessage(String message) {
        this.message = message;
      }

      public String getData() {
        return data;
      }

      public void setData(String data) {
        this.data = data;
      }
    }
  }

  private static class JsonRpcResponseSerializer extends StdSerializer<JsonRpcResponse> {
    private static final long serialVersionUID = -455832214649315448L;

    public JsonRpcResponseSerializer() {
      super(JsonRpcResponse.class);
    }

    @Override
    public void serialize(JsonRpcResponse value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {

      gen.writeStartObject();
      gen.writeStringField("jsonrpc", value.getJsonrpc());
      if (value.getError() != null) {
        gen.writeObjectField("error", value.getError());
      } else {
        gen.writeObjectField("result", value.getResult());
      }

      JsonNode idNode = value.getId();
      if (idNode == null || idNode.isNull()) {
        gen.writeNullField("id");
      } else {

        JsonNodeType valueType = idNode.getNodeType();
        if (valueType == JsonNodeType.NUMBER) {
          gen.writeNumberField("id", idNode.asLong());
        } else if (valueType == JsonNodeType.STRING) {
          gen.writeStringField("id", idNode.asText());
        } else {
          gen.writeNullField("id");
        }
      }

      gen.writeEndObject();
    }
  }
}
