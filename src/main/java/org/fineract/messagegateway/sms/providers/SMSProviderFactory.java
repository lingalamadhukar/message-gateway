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
package org.fineract.messagegateway.sms.providers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.fineract.messagegateway.exception.MessageGatewayException;
import org.fineract.messagegateway.sms.constants.SmsConstants;
import org.fineract.messagegateway.sms.domain.ExternalService;
import org.fineract.messagegateway.sms.domain.ExternalServiceProperties;
import org.fineract.messagegateway.sms.domain.InboundMessage;
import org.fineract.messagegateway.sms.domain.SMSBridge;
import org.fineract.messagegateway.sms.domain.SMSMessage;
import org.fineract.messagegateway.sms.exception.ProviderNotDefinedException;
import org.fineract.messagegateway.sms.exception.SMSBridgeNotFoundException;
import org.fineract.messagegateway.sms.repository.ExternalServicePropertiesRepository;
import org.fineract.messagegateway.sms.repository.ExternalServiceRepository;
import org.fineract.messagegateway.sms.repository.SMSBridgeRepository;
import org.fineract.messagegateway.sms.util.HttpClientUtil;
import org.fineract.messagegateway.sms.util.SmsMessageStatusType;
import org.fineract.messagegateway.tenants.domain.Tenant;
import org.fineract.messagegateway.tenants.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.gson.JsonObject;

@Component
public class SMSProviderFactory implements ApplicationContextAware {

	 private static final Logger logger = LoggerFactory.getLogger(SMSProviderFactory.class);
	 
	private ApplicationContext applicationContext;

	private final SMSBridgeRepository smsBridgeRepository;
	
	private final ExternalServiceRepository externalServiceRepository;
	
	private final TenantRepository tenantRepository;
	
	private final ExternalServicePropertiesRepository externalServicePropertiesRepository;

	@Autowired
	public SMSProviderFactory(final SMSBridgeRepository smsBridgeRepository, final ExternalServiceRepository externalServiceRepository,
	        final TenantRepository tenantRepository, final ExternalServicePropertiesRepository externalServicePropertiesRepository) {
		this.smsBridgeRepository = smsBridgeRepository;
		this.externalServiceRepository = externalServiceRepository;
		this.tenantRepository = tenantRepository;
		this.externalServicePropertiesRepository = externalServicePropertiesRepository;
	}

	public SMSProvider getSMSProvider(final SMSMessage message) throws SMSBridgeNotFoundException, ProviderNotDefinedException {
		SMSBridge bridge = this.smsBridgeRepository.findByIdAndTenantId(message.getBridgeId(),
				message.getTenantId());
		if (bridge == null) {
			throw new SMSBridgeNotFoundException(message.getBridgeId());
		}
		SMSProvider provider = (SMSProvider) this.applicationContext.getBean(bridge.getProviderKey()) ;
		if(provider == null) throw new ProviderNotDefinedException() ;
		return provider ;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void sendShortMessage(final SMSMessage message) {
		SMSBridge bridge = this.smsBridgeRepository.findByIdAndTenantId(message.getBridgeId(),
				message.getTenantId());
		SMSProvider provider = null;
		try {
			if (bridge == null) {
				throw new SMSBridgeNotFoundException(message.getBridgeId());
			}
			provider = (SMSProvider) this.applicationContext.getBean(bridge.getProviderKey()) ;
			if (provider == null) throw new ProviderNotDefinedException();
			provider.sendMessage(bridge, message);
			message.setDeliveryStatus(SmsMessageStatusType.SENT.getValue());
		} catch (SMSBridgeNotFoundException | MessageGatewayException | ProviderNotDefinedException | BeansException e) {
			logger.error(e.getMessage());
			message.setDeliveryErrorMessage(e.getMessage());
			message.setDeliveryStatus(SmsMessageStatusType.FAILED.getValue());
		}
	}
	
	public void sendShortMessage(final Collection<SMSMessage> messages) {
		for(SMSMessage message: messages) {
			SMSBridge bridge = this.smsBridgeRepository.findByIdAndTenantId(message.getBridgeId(),
					message.getTenantId());
			SMSProvider provider = null;
			try {
				if (bridge == null) {
					throw new SMSBridgeNotFoundException(message.getBridgeId());
				}
				provider = (SMSProvider) this.applicationContext.getBean(bridge.getProviderKey()) ;
				if (provider == null)
					throw new ProviderNotDefinedException();
				provider.sendMessage(bridge, message);
			} catch (SMSBridgeNotFoundException | MessageGatewayException | ProviderNotDefinedException | BeansException e) {
				logger.error(e.getMessage());
				message.setDeliveryErrorMessage(e.getMessage());
				message.setDeliveryStatus(SmsMessageStatusType.FAILED.getValue());
			}
		}
	}

    public void triggerInboundMessage(final InboundMessage message) {
        final Tenant tenant = this.tenantRepository.findOne(message.getTenantId());
        final String baseURL = getConfiguredExternalValue(SmsConstants.BASIC_URL, message.getTenantId());
        final String authenticationURI = getConfiguredExternalValue(SmsConstants.AUTHENTICATION_URI, message.getTenantId());
        final String smsURI = getConfiguredExternalValue(SmsConstants.SMS_URI, message.getTenantId());
        final ExternalService externalService = getExternalSerice(SmsConstants.SMS_USER_CREDENTIALS, message.getTenantId());
        final Map<String, String> credentials =getExternalPropertiesAsMap(externalService);
        final String username = credentials.get(SmsConstants.USERNAME);
        final String password = credentials.get(SmsConstants.PASSWORD);
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new RuntimeException("SMS user credentials are not mapped");
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("mobileNumber", message.getMobileNumber());
        payload.addProperty("ussdCode", message.getUssdCode());
        HttpClientUtil.sendInboundSMSRequest(tenant.getTenantId(), baseURL, authenticationURI, smsURI, payload.toString(), username, password);
    }

    public SMSProvider getSMSProvider(final String providerKey) throws ProviderNotDefinedException {
        final SMSProvider provider = (SMSProvider) this.applicationContext.getBean(providerKey);
        if (provider == null) { throw new ProviderNotDefinedException(); }
        return provider;
    }

    private String getConfiguredExternalValue(final String propery, final Long tenantId) {
        final ExternalService externalService = getExternalSerice(propery, tenantId);
        final Map<String, String> map = getExternalPropertiesAsMap(externalService);
        if (StringUtils.isBlank(map.get(propery))) { throw new RuntimeException("External server configuration "+propery+" is not configured"); }
        return map.get(propery);
    }
    
    private Map<String, String> getExternalPropertiesAsMap(final ExternalService externalService) {
        final List<ExternalServiceProperties> properties = this.externalServicePropertiesRepository.findByExternalServiceId(externalService.getId());
        Map<String, String> map = new HashMap<>();
        if (!CollectionUtils.isEmpty(properties)) {
            for (ExternalServiceProperties externalServiceProperties : properties) {
                map.put(externalServiceProperties.name(), externalServiceProperties.value());
            }
        }
        return map;
    }

    private ExternalService getExternalSerice(final String propery, final Long tenantId) {
        final ExternalService externalService = this.externalServiceRepository.findByNameAndTenantId(propery, tenantId);
        if (externalService == null) {
            throw new RuntimeException("External server configuration "+propery+" is not configured");
        }
        return externalService;
    }
}
