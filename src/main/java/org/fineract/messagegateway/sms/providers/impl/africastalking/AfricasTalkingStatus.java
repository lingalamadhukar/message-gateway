/**
 * Copyright (C) Conflux Technologies Pvt Ltd - All Rights Reserved
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is proprietary and confidential software; you can't redistribute it and/or modify it unless agreed to in writing.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 */
package org.fineract.messagegateway.sms.providers.impl.africastalking;

import org.fineract.messagegateway.sms.util.SmsMessageStatusType;

public class AfricasTalkingStatus {

    public static SmsMessageStatusType smsStatus(final String status) {
        SmsMessageStatusType smsStatus = SmsMessageStatusType.INVALID;
        switch (status.toUpperCase()) {
            case "SENT":
            case "BUFFERED":
            case "SUBMITTED":
                smsStatus = SmsMessageStatusType.SENT;
            break;
            case "SUCCESS":
                smsStatus = SmsMessageStatusType.DELIVERED;
            break;
            case "FAILED":
            case "REJECTED":
                smsStatus = SmsMessageStatusType.FAILED;
            break;
        }
        return smsStatus;
    }

}
