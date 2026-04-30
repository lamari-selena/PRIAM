package priam.data.priamdataservice.mappers;

import org.mapstruct.Mapper;
import priam.data.priamdataservice.entities.PersonalDataTransfer;
import priam.data.priamdataservice.entities.model.SecondaryActor;
import priam.data.priamdataservice.entities.model.SecondaryActorCategory;
import priam.data.priamdataservice.dto.*;
import priam.data.priamdataservice.dto.transfer.*;
@Mapper(componentModel = "spring")
public interface PersonalDataTransferMapper {
    PersonalDataTransferDTO TransferToTransferDTO(PersonalDataTransfer personalDataTransferDTO);

    PersonalDataTransfer TransferDTOToTransfer(PersonalDataTransferDTO personalDataTransferDTO);

    SecondaryActorDTO SecondaryActorToSecondaryActorDTO(SecondaryActor secondaryActor);

    SecondaryActor SecondaryActorDTOToSecondaryActor(SecondaryActorDTO secondaryActorDTO);

    SecondaryActorCategoryDTO SecondaryActorCategoryToSecondaryActorCategoryDTO(SecondaryActorCategory secondaryActorCategory);

    SecondaryActorCategory SecondaryActorCategoryDTOToSecondaryActorCategory(SecondaryActorCategoryDTO secondaryActorCategoryDTO);
}
