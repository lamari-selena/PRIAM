package priam.right.scheduler;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import priam.right.dto.NotificationRequestDTO;
import priam.right.entities.DataRequest;
import priam.right.openfeign.NotificationRestClient;
import priam.right.repositories.DataRequestRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Enforces GDPR Art. 12.3: requests must be answered within one month.
 * Runs daily and alerts the application owner for requests approaching their deadline.
 */
@Component
public class DeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineScheduler.class);

    private final DataRequestRepository dataRequestRepository;
    private final NotificationRestClient notificationRestClient;

    @Value("${notification.appOwnerEmail:owner@example.com}")
    private String appOwnerEmail;

    @Value("${notification.deadlineWarningDays:7}")
    private int deadlineWarningDays;

    public DeadlineScheduler(DataRequestRepository dataRequestRepository,
                             NotificationRestClient notificationRestClient) {
        this.dataRequestRepository = dataRequestRepository;
        this.notificationRestClient = notificationRestClient;
    }

    // Runs every day at 09:00
    @Scheduled(cron = "0 0 9 * * *")
    public void checkApproachingDeadlines() {
        // Threshold: requests issued more than (30 - warningDays) days ago and still unanswered
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -(30 - deadlineWarningDays));
        Date threshold = cal.getTime();

        List<DataRequest> pendingRequests =
                dataRequestRepository.findByResponseFalseAndDataRequestIssuedAtBefore(threshold);

        if (pendingRequests.isEmpty()) {
            log.info("DeadlineScheduler: no requests approaching the legal deadline.");
            return;
        }

        log.warn("DeadlineScheduler: {} request(s) are approaching the 1-month GDPR deadline.",
                pendingRequests.size());

        for (DataRequest request : pendingRequests) {
            long daysElapsed = (new Date().getTime() - request.getDataRequestIssuedAt().getTime())
                    / (1000L * 60 * 60 * 24);
            long daysRemaining = 30 - daysElapsed;

            NotificationRequestDTO notification = new NotificationRequestDTO(
                    "",
                    appOwnerEmail,
                    String.format(
                            "URGENT: rights request #%d (type: %s, subject ID: %d) has been pending for %d days. " +
                            "%d day(s) remaining before the legal deadline (GDPR Art. 12.3).",
                            request.getDataRequestId(),
                            request.getDataRequestType(),
                            request.getDataSubjectId(),
                            daysElapsed,
                            daysRemaining),
                    request.getDataRequestType().toString(),
                    "DEADLINE_WARNING"
            );

            try {
                notificationRestClient.sendNotification(notification);
                log.info("Deadline warning sent for request #{}.", request.getDataRequestId());
            } catch (Exception e) {
                log.error("Failed to send deadline warning for request #{}: {}",
                        request.getDataRequestId(), e.getMessage());
            }
        }
    }
}
