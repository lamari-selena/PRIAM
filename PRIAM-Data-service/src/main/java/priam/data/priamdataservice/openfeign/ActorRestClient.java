package priam.data.priamdataservice.openfeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import priam.data.priamdataservice.dto.DataSubjectResponseDTO;
import priam.data.priamdataservice.dto.SecondaryActorResponseDTO;
import priam.data.priamdataservice.entities.DataSubjectCategory;
import priam.data.priamdataservice.entities.model.SecondaryActor;

import java.util.List;

@FeignClient(name = "ACTOR-SERVICE")
public interface ActorRestClient {

    @GetMapping(path = "api/DataSubject/ref/{idRef}")
    public DataSubjectResponseDTO getDataSubjectByRef(@PathVariable String idRef);
    @GetMapping(path = "api/actor/DataSubjectCategory/{dataSubjectCategoryId}")
    public DataSubjectCategory getDataSubjectCategoryById(@PathVariable int dataSubjectCategoryId);
    @GetMapping(path = "api/DataSubject/{dataSubjectId}")
    public DataSubjectResponseDTO getDataSubjectId(@PathVariable int dataSubjectId);
    @GetMapping(path = "api/secondaryActor/{secondaryActorId}")
    public SecondaryActorResponseDTO getSecondaryActorId(@PathVariable int secondaryActorId);
    @GetMapping(path = "api/secondaryActors/dataId={dataId}")
    public List<SecondaryActor> getSecondaryActorsByDataId (@PathVariable int dataId);
}
