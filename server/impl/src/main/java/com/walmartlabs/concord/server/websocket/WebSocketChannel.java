package com.walmartlabs.concord.server.websocket;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
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
 * =====
 */

import com.walmartlabs.concord.server.queueclient.MessageSerializer;
import com.walmartlabs.concord.server.queueclient.message.Message;
import com.walmartlabs.concord.server.queueclient.message.MessageType;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketChannel {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannel.class);

    private final UUID channelId;
    private final String channelInfo;
    private final Session session;

    private final Map<Long, Message> requests = new ConcurrentHashMap<>();

    public WebSocketChannel(UUID channelId, Session session, String channelInfo) {
        this.channelId = channelId;
        this.session = session;
        this.channelInfo = channelInfo;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public String getInfo() {
        return channelInfo;
    }

    public void onRequest(Message request) {
        Message old = requests.put(request.getCorrelationId(), request);
        if (old != null) {
            log.error("request ['{}', '{}'] -> duplicate request. closing channel", channelId, request);
            close();
        }
    }

    /**
     * Sends the response and removes the associated request from the queue.
     */
    public boolean sendResponse(Message response) {
        if (!session.isOpen()) {
            log.warn("response ['{}', '{}'] -> session is closed", channelId, response);
            return false;
        }

        Message request = requests.remove(response.getCorrelationId());
        if (request == null) {
            log.warn("response ['{}', '{}'] -> request not found", channelId, response);
            return false;
        }

        try {
            session.getRemote().sendString(MessageSerializer.serialize(response));
            return true;
        } catch (Exception e) {
            log.error("response ['{}', '{}'] -> error", channelId, response, e);
            return false;
        }
    }

    public Message getRequest(MessageType requestType) {
        return requests.values().stream()
                .filter(m -> m.getMessageType() == requestType)
                .findAny().orElse(null);
    }

    public void close() {
        if (!session.isOpen()) {
            return;
        }

        try {
            session.close();
            session.disconnect();
        } catch (Exception e) {
            log.warn("close ['{}'] -> error", channelId, e);
        }
    }
}
