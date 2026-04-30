package priam.data.priamdataservice.openfeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import priam.data.priamdataservice.dto.DataSubjectResponseDTO;
import priam.data.priamdataservice.dto.SecondaryActorResponseDTO;
import priam.data.priamdataservice.entities.DataSubjectCategory;
import priam.data.priamdataservice.entities.model.SecondaryActor;

import java.util.List;

@FeignClient(name = "gateway", url = "${GATEWAY_URL}", contextId = "actorClient")
//@FeignClient(name = "ACTOR-SERVICE")
public interface ActorRestClient {


    //{/actor/} prefix
@GetMapping(path = "/actor/api/DataSubject/ref/{idRef}")
public DataSubjectResponseDTO getDataSubjectByRef(@PathVariable String idRef);
    @GetMapping(path = "/actor/api/actor/DataSubjectCategory/{dataSubjectCategoryId}")
    public DataSubjectCategory getDataSubjectCategoryById(@PathVariable int dataSubjectCategoryId);
    @GetMapping(path = "/actor/api/DataSubject/{dataSubjectId}")
    public DataSubjectResponseDTO getDataSubjectId(@PathVariable int dataSubjectId);
    @GetMapping(path = "/actor/api/secondaryActor/{secondaryActorId}")
    public SecondaryActorResponseDTO getSecondaryActorId(@PathVariable int secondaryActorId);
    @GetMapping(path = "/actor/api/secondaryActors/dataId={dataId}")
    public List<SecondaryActor> getSecondaryActorsByDataId (@PathVariable int dataId);
}
