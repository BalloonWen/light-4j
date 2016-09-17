package com.networknt.info;

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.sun.net.httpserver.HttpExchange;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This is a server info handler that output the runtime info about the server. For example, how many
 * components are installed and what is the cofiguration of each component. For handlers, it is registered
 * when injecting into the handler chain during server startup. For other utilities, it should have a
 * static block to register itself during server startup. Additional info is gathered from environment
 * variable and JVM.
 *
 * Created by steve on 17/09/16.
 */
public class ServerInfoHandler implements HttpHandler {
    public static final String CONFIG_NAME = "info";
    public static final String ENABLE_SERVER_INFO = "enableServerInfo";

    static final Logger logger = LoggerFactory.getLogger(ServerInfoHandler.class);

    private final HttpHandler next;

    public ServerInfoHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        Map<String, Object> infoMap = new LinkedHashMap<>();
        infoMap.put("environment", getEnvironment(exchange));
        infoMap.put("specification", Config.getInstance().getJsonMapConfigNoCache("swagger"));
        infoMap.put("component", ModuleRegistry.getRegistry());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(infoMap));
    }

    public Map<String, Object> getEnvironment(HttpServerExchange exchange) {
        Map<String, Object> envMap = new LinkedHashMap<>();
        envMap.put("host", getHost(exchange));
        envMap.put("runtime", getRuntime());
        envMap.put("system", getSystem());
        return envMap;
    }

    public Map<String, Object> getHost(HttpServerExchange exchange) {
        Map<String, Object> hostMap = new LinkedHashMap<>();
        String ip = "unknown";
        String hostname = "unknown";
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            ip = inetAddress.getHostAddress();
            hostname = inetAddress.getHostName();
        } catch (IOException ioe) {
            logger.error("Error in getting IP Address and Hostname", ioe);
        }
        hostMap.put("ip", ip);
        hostMap.put("hostname", hostname);
        hostMap.put("dns", exchange.getSourceAddress().getHostName());
        return hostMap;
    }

    public Map<String, Object> getRuntime() {
        Map<String, Object> runtimeMap = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        runtimeMap.put("availableProcessors", runtime.availableProcessors());
        runtimeMap.put("freeMemory", runtime.freeMemory());
        runtimeMap.put("totalMemory", runtime.totalMemory());
        runtimeMap.put("maxMemory", runtime.maxMemory());
        return runtimeMap;
    }

    public Map<String, Object> getSystem() {
        Map<String, Object> systemMap = new LinkedHashMap<>();
        Properties properties = System.getProperties();
        systemMap.put("javaVendor", properties.getProperty("java.vendor"));
        systemMap.put("javaVersion", properties.getProperty("java.version"));
        systemMap.put("osName", properties.getProperty("os.name"));
        systemMap.put("osVersion", properties.getProperty("os.version"));
        systemMap.put("userTimezone", properties.getProperty("user.timezone"));
        return systemMap;
    }
}
