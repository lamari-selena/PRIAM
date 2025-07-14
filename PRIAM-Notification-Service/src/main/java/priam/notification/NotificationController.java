package priam.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    @Autowired
    private KafkaProducerService kafkaProducerService;

    @PostMapping("/send")
    public String sendNotification(@RequestBody PrivacyRequest request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String message = objectMapper.writeValueAsString(request);
            log.info("Sending notification to Kafka Topic: {}", message);
            kafkaProducerService.sendMessage(message);
            log.info("Notification sent to Kafka Topic: {}", message);
            return "Notification sent successfully";
        } catch (JsonProcessingException e) {
            return "Error processing the notification request";
        }
    }
}

