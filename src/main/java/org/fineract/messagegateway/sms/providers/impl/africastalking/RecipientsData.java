package org.fineract.messagegateway.sms.providers.impl.africastalking;

public class RecipientsData {

    private String number;
    private String status;
    private String cost;
    private String messageId;
    private String failureReason;

    public String getNumber() {
        return this.number;
    }

    public void setNumber(final String number) {
        this.number = number;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getCost() {
        return this.cost;
    }

    public void setCost(final String cost) {
        this.cost = cost;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getFailureReason() {
        return this.failureReason;
    }

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

}
