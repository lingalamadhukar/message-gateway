/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fineract.messagegateway.sms.providers.impl.africastalking;

import org.fineract.messagegateway.constants.MessageGatewayConstants;
import org.fineract.messagegateway.exception.MessageGatewayException;
import org.fineract.messagegateway.sms.domain.SMSBridge;
import org.fineract.messagegateway.sms.domain.SMSMessage;
import org.fineract.messagegateway.sms.providers.SMSProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service(value = "AfricasTalking")
public class AfricasTalkingMessageProvider extends SMSProvider {

    private static final Logger logger = LoggerFactory.getLogger(AfricasTalkingMessageProvider.class);

    private final StringBuilder builder = new StringBuilder();

    public AfricasTalkingMessageProvider() {
        super();
    }

    @Override
    public void sendMessage(final SMSBridge smsBridgeConfig, final SMSMessage message) throws MessageGatewayException {
        final String userName = smsBridgeConfig.getConfigValue(MessageGatewayConstants.PROVIDER_ACCOUNT_ID);
        final String apiKey = smsBridgeConfig.getConfigValue(MessageGatewayConstants.PROVIDER_AUTH_TOKEN);
        final AfricasTalkingGateway gateway = new AfricasTalkingGateway(userName, apiKey);
        this.builder.setLength(0);
        this.builder.append(smsBridgeConfig.getCountryCode());
        this.builder.append(message.getMobileNumber());
        final String mobile = this.builder.toString();
        logger.info("Sending SMS to " + mobile + " ...");
        try {
            final JSONArray recipients = gateway.sendMessage(mobile, message.getMessage());
            final JSONObject result = recipients.getJSONObject(0);
            message.setExternalId(result.getString("messageId"));
            message.setDeliveryStatus(AfricasTalkingStatus.smsStatus(result.getString("status")).getValue());
        } catch (final Exception e) {
            throw new MessageGatewayException(e.getMessage());
        }
    }

}
