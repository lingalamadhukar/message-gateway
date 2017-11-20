--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

CREATE TABLE `m_external_service` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`tenant_id` BIGINT(20) NOT NULL,
	`name` VARCHAR(100) NOT NULL,
	PRIMARY KEY (`id`),
	UNIQUE INDEX `name_UNIQUE` (`name`, `tenant_id`),
	INDEX `FK_m_external_service_m_tenant_id` (`tenant_id`),
	CONSTRAINT `FK_m_external_service_m_tenant_id` FOREIGN KEY (`tenant_id`) REFERENCES `m_tenants` (`id`)
);

CREATE TABLE `m_external_service_properties` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`external_service_id` BIGINT(20) NOT NULL,
	`name` VARCHAR(150) NOT NULL,
	`value` VARCHAR(250) NULL DEFAULT NULL,
	PRIMARY KEY (`id`),
	UNIQUE INDEX `UQ_m_external_service_properties` (`name`, `external_service_id`),
	INDEX `FK_m_external_service_properties_m_external_service` (`external_service_id`),
	CONSTRAINT `FK_m_external_service_properties_m_external_service` FOREIGN KEY (`external_service_id`) REFERENCES `m_external_service` (`id`)
);

CREATE TABLE `m_inbound_messages` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`tenant_id` BIGINT(20) NOT NULL,
	`submitted_on_date` TIMESTAMP NOT NULL,
	`mobile_number` VARCHAR(50) NOT NULL,
	`ussd_code` VARCHAR(50) NULL DEFAULT NULL,
	PRIMARY KEY (`id`),
	INDEX `FK_m_tenants_m_inbound_messages` (`tenant_id`),
	CONSTRAINT `FK_m_tenants_m_inbound_messages` FOREIGN KEY (`tenant_id`) REFERENCES `m_tenants` (`id`)
);

INSERT IGNORE INTO `m_external_service` (`tenant_id`, `name`) VALUES
 ((SELECT id FROM m_tenants WHERE tenant_id = 'default'), 'BASIC_URL'),
 ((SELECT id FROM m_tenants WHERE tenant_id = 'default'), 'SMS_PROVIDER'),
 ((SELECT id FROM m_tenants WHERE tenant_id = 'default'), 'AUTHENTICATION_URI'),
 ((SELECT id FROM m_tenants WHERE tenant_id = 'default'), 'SMS_URI'),
 ((SELECT id FROM m_tenants WHERE tenant_id = 'default'), 'SMS_USER_CREDENTIALS');

INSERT IGNORE INTO `m_external_service_properties` (`external_service_id`, `name`, `value`) VALUES
 ((SELECT id FROM m_external_service WHERE name='BASIC_URL'), 'BASIC_URL', 'https://localhost:8443/fineract-provider/api/v1/'),
 ((SELECT id FROM m_external_service WHERE name='SMS_PROVIDER'), 'SMS_PROVIDER', 'AfricasTalking'),
 ((SELECT id FROM m_external_service WHERE name='AUTHENTICATION_URI'), 'AUTHENTICATION_URI', 'authentication'),
 ((SELECT id FROM m_external_service WHERE name='SMS_URI'), 'SMS_URI', 'sms/receivesms'),
 ((SELECT id FROM m_external_service WHERE name='SMS_USER_CREDENTIALS'), 'password', 'password'),
 ((SELECT id FROM m_external_service WHERE name='SMS_USER_CREDENTIALS'), 'username', 'mifos');

