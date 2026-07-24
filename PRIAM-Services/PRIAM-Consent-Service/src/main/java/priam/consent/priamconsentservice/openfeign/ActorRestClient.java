package priam.consent.priamconsentservice.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import priam.consent.priamconsentservice.entities.DataSubject;

@FeignClient(name = "gateway", contextId = "actorClient")
//@FeignClient(name = "ACTOR-SERVICE")
public interface ActorRestClient {

    //{/actor} prefix
    @GetMapping(path = "/actor/api/DataSubject/{id}")
    DataSubject getDataSubjectId(@PathVariable(name = "id") int idDataSubject);

    @GetMapping(path = "/actor/api/DataSubject/ref/{idRef}")
    DataSubject getDataSubjectByRef(@PathVariable (name = "idRef")String idRef);

    @GetMapping(path = "/actor/api/DataSubjectId/{idRef}")
    int getDataSubjectIdByIdRef(@PathVariable String idRef);

}
