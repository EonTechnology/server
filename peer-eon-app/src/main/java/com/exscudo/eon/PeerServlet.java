package com.exscudo.eon;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.exscudo.jsonrpc.JrpcService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * Servlet for peer requests.
 * <p>
 * Uses {@link JrpcService} for handling requests.
 *
 * @see JrpcService
 */
public class PeerServlet extends FrameworkServlet {
    private static final long serialVersionUID = -7244549261237438100L;

    private JrpcService service = null;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        WebApplicationContext context = getWebApplicationContext();
        service = (JrpcService) context.getBean("Service");
    }

    @Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {

        service.setRemoteHost(request.getRemoteHost());

        InputStream inputStream = request.getInputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int numberOfBytes;
        while ((numberOfBytes = inputStream.read(buffer)) > 0) {

            byteArrayOutputStream.write(buffer, 0, numberOfBytes);
        }
        inputStream.close();

        String responseBody = service.doRequest(byteArrayOutputStream.toString("UTF-8"));
        byte[] responseBytes = responseBody.getBytes("UTF-8");

        response.setContentType("text/plain; charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        ServletOutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write(responseBytes);
        servletOutputStream.close();
    }
}
