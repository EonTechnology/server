package org.eontechnology.and.eon.app.jsonrpc;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import org.eontechnology.and.jsonrpc.CompositeInnerService;
import org.eontechnology.and.jsonrpc.JrpcService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ProxyServiceTest {

  private HttpURLConnection mockConnection;
  private URL mockURL;

  @Before
  public void setUp() throws Exception {
    mockConnection = mock(HttpURLConnection.class);

    mockURL =
        new URL(
            "http",
            "127.0.0.1",
            80,
            "file",
            new URLStreamHandler() {
              @Override
              protected URLConnection openConnection(URL u) throws IOException {
                return mockConnection;
              }
            });
  }

  @Test
  public void method() throws Exception {
    Buffer buffer = new Buffer();
    when(mockConnection.getOutputStream()).thenReturn(buffer.outputStream);
    when(mockConnection.getResponseCode())
        .then(
            new Answer<Integer>() {

              @Override
              public Integer answer(InvocationOnMock invocation) throws Throwable {
                buffer.apply();
                return HttpURLConnection.HTTP_OK;
              }
            });
    when(mockConnection.getInputStream()).thenReturn(buffer.inputStream);

    Map<String, String> map = new HashMap<>();
    map.put(IProxyServiceInterface.class.getName(), "service");

    Map<String, String> mapImpl = new HashMap<>();
    mapImpl.put(IProxyServiceInterface.class.getName(), ProxyServiceImpl.class.getName());

    JrpcServiceProxyFactory factory = new JrpcServiceProxyFactory(map, mapImpl);
    IProxyServiceInterface itf = factory.createProxy(mockURL, IProxyServiceInterface.class);
    assertTrue(itf.method(12345, "value"));
  }

  @Test
  public void to_string() throws Exception {

    Map<String, String> map = new HashMap<>();
    map.put(IProxyServiceInterface.class.getName(), "service");

    Map<String, String> mapImpl = new HashMap<>();
    mapImpl.put(IProxyServiceInterface.class.getName(), ProxyServiceImpl.class.getName());

    JrpcServiceProxyFactory factory = new JrpcServiceProxyFactory(map, mapImpl);
    IProxyServiceInterface itf = factory.createProxy(mockURL, IProxyServiceInterface.class);
    String value = itf.toString();
    assertTrue(value.contains("ProxyServiceImpl"));
  }

  class Buffer {

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    JrpcService service;
    OutputStream outputStream =
        new OutputStream() {
          @Override
          public void write(int b) throws IOException {
            throw new UnsupportedOperationException();
          }

          @Override
          public synchronized void write(byte[] bytes) throws IOException {
            byteArrayOutputStream.write(bytes);
          }
        };
    InputStreamWrapper inputStream = new InputStreamWrapper();

    public Buffer() {
      Map<String, Object> map = new HashMap<>();
      map.put("service", new ProxyServiceImpl());
      service = new JrpcService(new CompositeInnerService(map));
    }

    public void apply() throws UnsupportedEncodingException, IOException {
      String response = service.doRequest(byteArrayOutputStream.toString());
      inputStream.stream = new ByteArrayInputStream(response.getBytes());
    }

    class InputStreamWrapper extends InputStream {
      public InputStream stream;

      @Override
      public int read() throws IOException {
        return stream.read();
      }
    }
  }
}
