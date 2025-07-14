package priam.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    @Autowired
    private EmailService emailService;

    @KafkaListener(topics = "privacy-requests", groupId = "notification-group")
    public void consume(String message) {
        try {
            log.info("Sending message: {}", message);
            ObjectMapper objectMapper = new ObjectMapper();
            PrivacyRequest request = objectMapper.readValue(message, PrivacyRequest.class);

            // Send email to data subject
            emailService.sendSimpleMessage(
                    request.getDataSubjectEmail(),
                    "Notification: " + request.getRequestType(),
                    request.getMessage()
            );
            log.info("Sent message to data subject: {}", request.getDataSubjectEmail());
            // Send email to app provider
            emailService.sendSimpleMessage(
                    request.getAppProviderEmail(),
                    "Notification: " + request.getRequestType(),
                    request.getMessage()
            );
            log.info("Sent message to app provider: {}", request.getAppProviderEmail());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
