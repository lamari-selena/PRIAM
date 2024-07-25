package priam.consent.priamconsentservice.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import priam.consent.priamconsentservice.entities.DataSubject;

@FeignClient(name = "ACTOR-SERVICE")
public interface ActorRestClient {

    @GetMapping(path = "/api/DataSubject/{id}")
    DataSubject getDataSubject(@PathVariable(name = "id") int idDataSubject);

    @GetMapping(path = "/api/DataSubject/ref/{idRef}")
    DataSubject getDataSubjectByRef(@PathVariable (name = "idRef")String idRef);

    @GetMapping(path = "/api/DataSubjectId/{idRef}")
    int getDataSubjectIdByIdRef(@PathVariable String idRef);
}
