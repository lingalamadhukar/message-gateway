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
package org.fineract.messagegateway.sms.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.fineract.messagegateway.service.SecurityService;
import org.fineract.messagegateway.sms.constants.SmsConstants;
import org.fineract.messagegateway.sms.data.DeliveryStatusData;
import org.fineract.messagegateway.sms.domain.ExternalService;
import org.fineract.messagegateway.sms.domain.ExternalServiceProperties;
import org.fineract.messagegateway.sms.domain.InboundMessage;
import org.fineract.messagegateway.sms.domain.SMSMessage;
import org.fineract.messagegateway.sms.exception.ProviderNotDefinedException;
import org.fineract.messagegateway.sms.providers.SMSProvider;
import org.fineract.messagegateway.sms.providers.SMSProviderFactory;
import org.fineract.messagegateway.sms.repository.ExternalServicePropertiesRepository;
import org.fineract.messagegateway.sms.repository.ExternalServiceRepository;
import org.fineract.messagegateway.sms.repository.InboundMessageRepository;
import org.fineract.messagegateway.sms.repository.SmsOutboundMessageRepository;
import org.fineract.messagegateway.sms.util.SmsMessageStatusType;
import org.fineract.messagegateway.tenants.domain.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class SMSMessageService {

	 private static final Logger logger = LoggerFactory.getLogger(SMSMessageService.class);
	 
	private final SmsOutboundMessageRepository smsOutboundMessageRepository ;
	
	private final SMSProviderFactory smsProviderFactory ;
	
	private final JdbcTemplate jdbcTemplate ;
	
	private ExecutorService executorService ;
	
	private ScheduledExecutorService scheduledExecutorService ;
	
	private final SecurityService securityService ;
	
	private final ExternalServiceRepository externalServiceRepository;
	
	private final InboundMessageRepository inboundMessageRepository;
	
	private final ExternalServicePropertiesRepository externalServicePropertiesRepository;
	
	@Autowired
	public SMSMessageService(final SmsOutboundMessageRepository smsOutboundMessageRepository,
			final SMSProviderFactory smsProviderFactory,
			final DataSource dataSource,
			final SecurityService securityService,
			final ExternalServiceRepository externalServiceRepository,
			final InboundMessageRepository inboundMessageRepository,
			final ExternalServicePropertiesRepository externalServicePropertiesRepository) {
		this.smsOutboundMessageRepository = smsOutboundMessageRepository ;
		this.smsProviderFactory = smsProviderFactory ;
		this.jdbcTemplate = new JdbcTemplate(dataSource) ;
		this.securityService = securityService ;
		this.externalServiceRepository = externalServiceRepository;
		this.inboundMessageRepository = inboundMessageRepository;
		this.externalServicePropertiesRepository = externalServicePropertiesRepository;
	}
	
	@PostConstruct
	public void init() {
		logger.debug("Intializing SMSMessage Service.....");
		executorService = Executors.newSingleThreadExecutor();
		scheduledExecutorService = Executors.newSingleThreadScheduledExecutor() ;
		scheduledExecutorService.schedule(new BootupPendingMessagesTask(this.smsOutboundMessageRepository, this.smsProviderFactory) , 1, TimeUnit.MINUTES) ;
		//When do I have to shutdown  scheduledExecutorService ? :-( as it is no use after triggering BootupPendingMessagesTask
		//Shutdown scheduledExecutorService on application close event
	}
	
	public void sendShortMessage(final String tenantId, final String tenantAppKey, final Collection<SMSMessage> messages) {
		logger.debug("Request Received to send messages.....");
		Tenant tenant = this.securityService.authenticate(tenantId, tenantAppKey) ;
		for(SMSMessage message: messages) {
			message.setTenant(tenant.getId());
		}
		this.smsOutboundMessageRepository.save(messages) ;
		this.executorService.execute(new MessageTask(tenant, this.smsOutboundMessageRepository, this.smsProviderFactory, messages));
	}
	
	public Collection<DeliveryStatusData> getDeliveryStatus(final String tenantId, final String tenantAppKey, final Collection<Long> internalIds) {
		Tenant tenant = this.securityService.authenticate(tenantId, tenantAppKey) ;
		DeliveryStatusDataRowMapper mapper = new DeliveryStatusDataRowMapper() ;
		String internaIdString = internalIds.toString() ;
		internaIdString = internaIdString.replace("[", "(") ;
		internaIdString = internaIdString.replace("]", ")") ;
		String query = mapper.schema() + " where m.tenant_id=?"+" and m.internal_id in " +internaIdString;
		Collection<DeliveryStatusData> datas = this.jdbcTemplate.query(query, mapper, new Object[] {tenant.getId()}) ;
		return datas ;
	}
	
	class DeliveryStatusDataRowMapper implements RowMapper<DeliveryStatusData> {

		private final StringBuilder buff = new StringBuilder() ;
		
		public DeliveryStatusDataRowMapper() {
			buff.append("select internal_id, external_id, delivered_on_date, delivery_status, delivery_error_message from m_outbound_messages m") ;
		}
		
		public String schema() {
			return buff.toString() ;
		}
		
		@Override
		public DeliveryStatusData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException { 
			String internalId = rs.getString("internal_id") ;
			String externalId = rs.getString("external_id") ;
			Date deliveredOnDate = rs.getDate("delivered_on_date") ;
			Integer deliveryStatus = rs.getInt("delivery_status") ;
			String errorMessage = rs.getString("delivery_error_message") ;
			DeliveryStatusData data = new DeliveryStatusData(internalId, externalId, deliveredOnDate, deliveryStatus, errorMessage) ;
			return data;
		}
	}
	
	class MessageTask implements Runnable {

		final Collection<SMSMessage> messages ;
		final SmsOutboundMessageRepository smsOutboundMessageRepository ;
		final SMSProviderFactory smsProviderFactory ;
		final Tenant tenant ;
		
		public MessageTask(final Tenant tenant, final SmsOutboundMessageRepository smsOutboundMessageRepository, 
				final SMSProviderFactory smsProviderFactory,
				final Collection<SMSMessage> messages) {
			this.tenant = tenant ;
			this.messages = messages ;
			this.smsOutboundMessageRepository = smsOutboundMessageRepository ;
			this.smsProviderFactory = smsProviderFactory ;
		}
		
		@Override
		public void run() {
			this.smsProviderFactory.sendShortMessage(messages);
			this.smsOutboundMessageRepository.save(messages) ;
		}
	}
	
	class BootupPendingMessagesTask implements Callable<Integer> {

		final SmsOutboundMessageRepository smsOutboundMessageRepository ;
		final SMSProviderFactory smsProviderFactory ;
		public BootupPendingMessagesTask(final SmsOutboundMessageRepository smsOutboundMessageRepository, 
				final SMSProviderFactory smsProviderFactory) {
			this.smsOutboundMessageRepository = smsOutboundMessageRepository ;
			this.smsProviderFactory = smsProviderFactory ;
		}

		@Override
		public Integer call() throws Exception {
			logger.info("Sending Pending Messages on bootup.....");
			Integer page = 0;
			Integer initialSize = 200;
			Integer totalPageSize = 0;
			do {
				PageRequest pageRequest = new PageRequest(page, initialSize);
				Page<SMSMessage> messages = this.smsOutboundMessageRepository.findByDeliveryStatus(SmsMessageStatusType.PENDING.getValue(), pageRequest) ;
				page++;
				totalPageSize = messages.getTotalPages();
				this.smsProviderFactory.sendShortMessage(messages.getContent());
				this.smsOutboundMessageRepository.save(messages) ;
			}while (page < totalPageSize);
			return totalPageSize;
		}
	}

    public void triggerInboundMessage(final String tenantIdentifier, final String payload) {
        logger.debug("Request Received to inbound messages.....");
        final Tenant tenant = this.securityService.findTenantWithNotFoundDetection(tenantIdentifier);
        final ExternalService externalService = this.externalServiceRepository.findByNameAndTenantId(SmsConstants.SMS_PROVIDER,
                tenant.getId());
        if (externalService == null) { throw new RuntimeException("External Service Not found"); }
        final List<ExternalServiceProperties> properties = this.externalServicePropertiesRepository.findByExternalServiceId(externalService.getId());
        String smsProvider = null;
        for (ExternalServiceProperties externalServiceProperties : properties) {
            if (externalServiceProperties.name().equalsIgnoreCase(SmsConstants.SMS_PROVIDER)) {
                smsProvider = externalServiceProperties.value();
                break;
            }
        }
        if (StringUtils.isBlank(smsProvider)) { throw new RuntimeException("SMS Provider is not configured"); }
        try {
            SMSProvider provider = this.smsProviderFactory.getSMSProvider(smsProvider);
            InboundMessage message = provider.createInboundMessage(tenant.getId(), payload);
            if (message == null) {
                throw new RuntimeException("Message Inbound Provider is not implemented");
            }
            this.inboundMessageRepository.save(message);
            this.executorService.execute(new InboundMessageTask(this.smsProviderFactory, message));
        } catch (ProviderNotDefinedException e) {
            e.printStackTrace();
            throw new RuntimeException("Sms Provider not configured");
        }
    }

    class InboundMessageTask implements Runnable {

        final InboundMessage message;
        final SMSProviderFactory smsProviderFactory;

        public InboundMessageTask(final SMSProviderFactory smsProviderFactory, final InboundMessage message) {
            this.smsProviderFactory = smsProviderFactory;
            this.message = message;
        }

        @Override
        public void run() {
            this.smsProviderFactory.triggerInboundMessage(message);
        }
    }
}
