package priam.data.priamdataservice.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "RIGHT-SERVICE")
public interface RightRestClient {
    //getIfDataAccessAccepted
    @GetMapping(path = "api/isAccepted")
    public boolean isDataRequestAcceptedForDataId(@RequestParam int dataSubjectId, @RequestParam int dataId);
}

