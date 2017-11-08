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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.fineract.messagegateway.sms.domain.SMSMessage;
import org.fineract.messagegateway.sms.repository.SmsOutboundMessageRepository;
import org.fineract.messagegateway.sms.util.SmsMessageStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/africastalking")
public class AfricasTalkingApiResource {

    private static final Logger logger = LoggerFactory.getLogger(AfricasTalkingApiResource.class);

    private final SmsOutboundMessageRepository smsOutboundMessageRepository;

    @Autowired
    public AfricasTalkingApiResource(final SmsOutboundMessageRepository smsOutboundMessageRepository) {
        this.smsOutboundMessageRepository = smsOutboundMessageRepository;
    }

    @RequestMapping(value = "/report", method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" })
    public ResponseEntity<Void> updateDeliveryStatus(@RequestBody final AfricasTalkingResponseData payload) {
        if (null != payload) {
            final List<RecipientsData> recipients = payload.getRecipients();
            if (!CollectionUtils.isEmpty(recipients)) {
                for (final RecipientsData recipient : recipients) {
                    updateMessageDeliveryStatus(recipient);
                }
            }
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void updateMessageDeliveryStatus(final RecipientsData recipient) {
        final String externalId = recipient.getMessageId();
        if (StringUtils.isNotBlank(externalId)) {
            final SMSMessage message = this.smsOutboundMessageRepository.findByExternalId(externalId);
            if (message != null) {
                logger.debug("Status Callback received from AfricasTalking for " + externalId + " with status:" + recipient.getStatus());
                final SmsMessageStatusType status = AfricasTalkingStatus.smsStatus(recipient.getStatus());
                message.setDeliveryStatus(status.getValue());
                if (status.isFailed()) {
                    message.setDeliveryErrorMessage(recipient.getFailureReason());
                }
                this.smsOutboundMessageRepository.save(message);
            }
        }
    }
}
