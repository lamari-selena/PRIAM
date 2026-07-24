package priam.data.priamdataservice.mappers;

import org.mapstruct.Mapper;
import priam.data.priamdataservice.dto.transfer.PersonalDataTransferDTO;
import priam.data.priamdataservice.dto.transfer.SecondaryActorCategoryDTO;
import priam.data.priamdataservice.dto.transfer.SecondaryActorDTO;
import priam.data.priamdataservice.entities.PersonalDataTransfer;
import priam.data.priamdataservice.entities.model.SecondaryActor;
import priam.data.priamdataservice.entities.model.SecondaryActorCategory;

@Mapper(componentModel = "spring")
public interface PersonalDataTransferMapper {
    PersonalDataTransferDTO TransferToTransferDTO(PersonalDataTransfer personalDataTransferDTO);

    PersonalDataTransfer TransferDTOToTransfer(PersonalDataTransferDTO personalDataTransferDTO);

    SecondaryActorDTO SecondaryActorToSecondaryActorDTO(SecondaryActor secondaryActor);

    SecondaryActor SecondaryActorDTOToSecondaryActor(SecondaryActorDTO secondaryActorDTO);

    SecondaryActorCategoryDTO SecondaryActorCategoryToSecondaryActorCategoryDTO(SecondaryActorCategory secondaryActorCategory);

    SecondaryActorCategory SecondaryActorCategoryDTOToSecondaryActorCategory(SecondaryActorCategoryDTO secondaryActorCategoryDTO);
}
