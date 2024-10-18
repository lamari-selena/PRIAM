package priam.actor.web;

import org.springframework.web.bind.annotation.*;
import priam.actor.dto.SecondaryActorRequestDTO;
import priam.actor.dto.SecondaryActorResponseDTO;
import priam.actor.entities.SecondaryActor;
import priam.actor.mappers.SecondaryActorMapper;
import priam.actor.services.SecondaryActorService;

import java.util.List;

@RestController
@RequestMapping(path = "/api")
public class SecondaryActorRestAPI {
    private final SecondaryActorService secondaryActorService;


    public SecondaryActorRestAPI(SecondaryActorService secondaryActorService) {
        this.secondaryActorService = secondaryActorService;
    }


    @PostMapping(path = "/secondaryActor")
    public SecondaryActorResponseDTO createSecondaryActor(@RequestBody SecondaryActorRequestDTO secondaryActorRequestDTO) {
        return secondaryActorService.saveSecondaryActor(secondaryActorRequestDTO);
    }


    @GetMapping(path = "/secondaryActor/{secondaryActorId}")
    public SecondaryActorResponseDTO getSecondaryActorId(@PathVariable int secondaryActorId) {
        return secondaryActorService.findSecondaryActor(secondaryActorId);
    }

    @GetMapping(path = "/secondaryActors/dataId={dataId}")
    public List<SecondaryActor> getSecondaryActorsByDataId (@PathVariable int dataId){
        return secondaryActorService.getSecondaryActorsByDataId(dataId);
    }

}
