package priam.actor.services;

import priam.actor.dto.DataSubjectRequestDTO;
import priam.actor.dto.DataSubjectResponseDTO;
import priam.actor.dto.SecondaryActorRequestDTO;
import priam.actor.dto.SecondaryActorResponseDTO;
import priam.actor.entities.SecondaryActor;

import java.util.List;

public interface SecondaryActorService {
    SecondaryActorResponseDTO saveSecondaryActor(SecondaryActorRequestDTO secondaryActorRequestDTO);

    /**
     * Retrieve a DataSubjectResponseDTO object based on a data subject ID
     *
     * @param secondaryActorId The data subject ID
     * @return A DataSubjectResponseDTO object
     */
    SecondaryActorResponseDTO findSecondaryActor(int secondaryActorId);
    public List<SecondaryActor> getSecondaryActorsByDataId (int dataId);
}
