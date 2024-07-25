package priam.data.priamdataservice.openfeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import priam.data.priamdataservice.dto.DataSubjectResponseDTO;
import priam.data.priamdataservice.entities.DataSubjectCategory;

@FeignClient(name = "ACTOR-SERVICE")
public interface ActorRestClient {

    @GetMapping(path = "api/DataSubject/ref/{idRef}")
    public DataSubjectResponseDTO getDataSubjectByRef(@PathVariable String idRef);
    @GetMapping(path = "api/actor/DataSubjectCategory/{dataSubjectCategoryId}")
    public DataSubjectCategory getDataSubjectCategoryById(@PathVariable int dataSubjectCategoryId);
}
