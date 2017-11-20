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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "m_inbound_messages")
public class InboundMessage extends AbstractPersistableCustom<Long> {

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "submitted_on_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedOnDate;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "ussd_code", nullable = false)
    private String ussdCode;

    protected InboundMessage() {

    }

    private InboundMessage(final Long tenantId, final String mobileNumber, final String ussdCode) {
        this.tenantId = tenantId;
        this.mobileNumber = mobileNumber;
        this.submittedOnDate = new Date();
        this.ussdCode = ussdCode;
    }

    public static InboundMessage getInstance(final Long tenantId, final String mobileNumber,
            final String ussdCode) {

        return new InboundMessage(tenantId, mobileNumber, ussdCode);
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public void setTenant(final Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getMobileNumber() {
        return this.mobileNumber;
    }

    public void setMobileNumber(final String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getUssdCode() {
        return this.ussdCode;
    }

    public void setMessage(final String ussdCode) {
        this.ussdCode = ussdCode;
    }

    public void setSubmittedOnDate(final Date submittedDate) {
        this.submittedOnDate = submittedDate;
    }

    @Override
    public String toString() {
        return "SmsInboundMessage [TenantIdentifier=" + this.tenantId + ", submittedOnDate=" + this.submittedOnDate + ", mobileNumber="
                + this.mobileNumber + ", ussdCode=" + this.ussdCode + "]";
    }
}
