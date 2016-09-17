package com.networknt.validator;

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a simple audit handler that dump most important info per request basis. The following
 * elements will be logged if it's available. This handle can be used on production for certain
 * applications that need audit on request. Turn off statusCode and responseTime can make it faster
 *
 * timestamp
 * correlationId
 * traceabilityId (if available)
 * clientId
 * userId (if available)
 * scopeClientId (available if called by an API)
 * endpoint (uriPattern@method)
 * statusCode
 * responseTime
 *
 * Created by steve on 17/09/16.
 */
public class SwaggerValidatorHandler implements HttpHandler {
    public static final String CONFIG_NAME = "audit";
    public static final String ENABLE_SIMPLE_AUDIT = "enableSimpleAudit";

    static final String SIMPLE = "simple";
    static final String HEADERS = "headers";
    static final String STATUS_CODE = "statusCode";
    static final String RESPONSE_TIME = "responseTime";
    static final String TIMESTAMPT = "timestamp";

    public static Map<String, Object> config;
    private static List<String> headerList;
    private static boolean statusCode = false;
    private static boolean responseTime = false;

    static final Logger audit = LoggerFactory.getLogger(Constants.AUDIT_LOGGER);

    private final HttpHandler next;

    static {
        config = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);
        Map<String, Object> simpleMap = (Map<String, Object>)config.get(SIMPLE);
        headerList = (List<String>)simpleMap.get(HEADERS);
        Object object = simpleMap.get(STATUS_CODE);
        if(object != null && (Boolean)object == true) {
            statusCode = true;
        }
        object = simpleMap.get(RESPONSE_TIME);
        if(object != null && (Boolean)object == true) {
            responseTime = true;
        }
    }

    public SwaggerValidatorHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final long start = System.currentTimeMillis();
        Map<String, Object> auditMap = new LinkedHashMap<>();
        auditMap.put(TIMESTAMPT, System.currentTimeMillis());
        // dump headers according to config
        if(headerList != null && headerList.size() > 0) {
            for(String name: headerList) {
                auditMap.put(name, exchange.getRequestHeaders().getFirst(name));
            }
        }
        if(statusCode || responseTime) {
            exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
                @Override
                public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
                    if(statusCode) {
                        auditMap.put(STATUS_CODE, exchange.getStatusCode());
                    }
                    if(responseTime) {
                        auditMap.put(RESPONSE_TIME, new Long(System.currentTimeMillis() - start));
                    }
                    nextListener.proceed();
                }
            });
        }
        audit.info(Config.getInstance().getMapper().writeValueAsString(auditMap));
        next.handleRequest(exchange);
    }

    private static class Wrapper implements HandlerWrapper {
        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new SwaggerValidatorHandler(handler);
        }
    }
}
