/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.traceability;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;

/**
 * This is a handler that checks if X-Traceability-Id exists in request header and put it into
 * response header if it exists.
 *
 * The traceability-id is set by the consumer and it will be passed to all services and returned
 * to the consumer eventually if there is no error. The AuditHandler will log it in audit log
 * and Client will pass it to the next service.
 *
 * Dependencies: AuditHandler, Client
 *
 * @author Steve Hu
 */
public class TraceabilityHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(TraceabilityHandler.class);
    private static final String TID = "tId";

    public static TraceabilityConfig config;

    private volatile HttpHandler next;

    public TraceabilityHandler() {
        config = TraceabilityConfig.load();
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled()) logger.trace("TraceabilityHandler.handleRequest starts.");
        String tid = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
        if(tid != null) {
            exchange.getResponseHeaders().put(HttpStringConstants.TRACEABILITY_ID, tid);
            this.addHandlerMDCContext(exchange, TID, tid);
            MDC.put(TID, tid);
        } else {
            MDC.remove(TID);
        }
        if (logger.isDebugEnabled()) logger.trace("TraceabilityHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(TraceabilityConfig.CONFIG_NAME, TraceabilityHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(TraceabilityConfig.CONFIG_NAME, TraceabilityHandler.class.getName(), config.getMappedConfig(), null);
        if(logger.isInfoEnabled()) logger.info("TraceabilityHandler is reloaded.");
    }
}
