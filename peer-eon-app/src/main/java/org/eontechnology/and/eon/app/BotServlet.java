package org.eontechnology.and.eon.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eontechnology.and.jsonrpc.JrpcService;
import org.eontechnology.and.jsonrpc.RequestContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * Servlet for bot requests
 * <p>
 * Uses {@link JrpcService} for handling requests.
 *
 * @see JrpcService
 */
public class BotServlet extends FrameworkServlet {
    private static final long serialVersionUID = -1912222673188737356L;

    private JrpcService service;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        WebApplicationContext context = getWebApplicationContext();
        service = (JrpcService) context.getBean("Service");
    }

    @Override
    public void doService(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        InputStream inputStream = request.getInputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[65536];
        int numberOfBytes;
        while ((numberOfBytes = inputStream.read(buffer)) > 0) {

            byteArrayOutputStream.write(buffer, 0, numberOfBytes);
        }
        inputStream.close();

        RequestContextHolder.setRemoteHost("Client");
        String responseBody = service.doRequest(byteArrayOutputStream.toString("UTF-8"));
        RequestContextHolder.resetRemoteHost();

        byte[] responseBytes = responseBody.getBytes("UTF-8");

        response.setContentType("application/json; charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        ServletOutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write(responseBytes);
        servletOutputStream.close();
    }
}
