package priam.notification;

public class PrivacyRequest {
    private String dataSubjectEmail;
    private String appProviderEmail;
    private String message;
    private String requestType;
    private String status;

    // Getters and setters
    public String getDataSubjectEmail() {
        return dataSubjectEmail;
    }

    public void setDataSubjectEmail(String dataSubjectEmail) {
        this.dataSubjectEmail = dataSubjectEmail;
    }

    public String getAppProviderEmail() {
        return appProviderEmail;
    }

    public void setAppProviderEmail(String appProviderEmail) {
        this.appProviderEmail = appProviderEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
