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
package org.fineract.messagegateway.sms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "m_external_service_properties", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "external_service_id" }, name = "UQ_m_external_service_properties") })
public class ExternalServiceProperties extends AbstractPersistableCustom<Long> {

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;

    @Column(name = "external_service_id")
    private Long externalServiceId;

    protected ExternalServiceProperties() {}

    public String name() {
        return this.name;
    }

    public String value() {
        return this.value;
    }

}