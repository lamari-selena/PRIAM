package priam.right.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import priam.right.dto.NotificationRequestDTO;

@FeignClient(name = "gateway", contextId = "notificationClient")
public interface NotificationRestClient {

    @PostMapping(path = "/notification/api/notifications/send")
    String sendNotification(@RequestBody NotificationRequestDTO notificationRequest);
}
