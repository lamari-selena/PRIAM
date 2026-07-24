package priam.right.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import priam.right.dto.DataSubjectCategoryResponseDTO;
import priam.right.entities.DataSubject;


//@FeignClient(name = "ACTOR-SERVICE")
@FeignClient(name = "gateway", contextId = "actorClient")
public interface ActorRestClient {

    //{/actor} prefix
    @GetMapping(path = "/actor/api/DataSubject/{dataSubjectId}")
    DataSubject getDataSubject(@PathVariable(name = "dataSubjectId") int dataSubjectId);

    @GetMapping(path = "/actor/api/DataSubject/ref/{idRef}")
    DataSubject getDataSubjectByRef(@PathVariable String idRef);

    @GetMapping(path = "/actor/api/actor/DataSubjectCategory/{dataSubjectCategoryId}")
    DataSubjectCategoryResponseDTO getDataSubjectCategoryById(@PathVariable int dataSubjectCategoryId);
}
