package org.fineract.messagegateway.sms.providers.impl.africastalking;

import java.util.List;

public class AfricasTalkingResponseData {

    private String Message;
    private List<RecipientsData> Recipients;

    public String getMessage() {
        return this.Message;
    }

    public void setMessage(final String message) {
        this.Message = message;
    }

    public List<RecipientsData> getRecipients() {
        return this.Recipients;
    }

    public void setRecipients(final List<RecipientsData> recipients) {
        this.Recipients = recipients;
    }
}
