package priam.right.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import priam.right.dto.DataSubjectCategoryResponseDTO;
import priam.right.entities.DataSubject;


@FeignClient(name = "ACTOR-SERVICE")
//@FeignClient(name = "ACTOR-SERVICE")
public interface ActorRestClient {

    @GetMapping(path = "/api/DataSubject/{dataSubjectId}")
    DataSubject getDataSubject(@PathVariable(name = "dataSubjectId") int dataSubjectId);

    @GetMapping(path = "/api/DataSubject/ref/{idRef}")
    DataSubject getDataSubjectByRef(@PathVariable String idRef);

    @GetMapping(path = "/api/actor/DataSubjectCategory/{dataSubjectCategoryId}")
    DataSubjectCategoryResponseDTO getDataSubjectCategoryById(@PathVariable int dataSubjectCategoryId);
}
