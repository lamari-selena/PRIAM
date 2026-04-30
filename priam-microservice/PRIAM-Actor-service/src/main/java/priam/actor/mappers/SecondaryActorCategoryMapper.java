package priam.actor.mappers;

import priam.actor.dto.DataSubjectCategoryRequestDTO;
import priam.actor.dto.DataSubjectCategoryResponseDTO;
import priam.actor.dto.SecondaryActorCategoryDTO;
import priam.actor.entities.DataSubjectCategory;
import priam.actor.entities.SecondaryActorCategory;

public interface SecondaryActorCategoryMapper {
    SecondaryActorCategoryDTO SecondaryActorCategoryToSecondaryActorCategoryResponseDTO(SecondaryActorCategory secondaryActorCategory);
    SecondaryActorCategory SecondaryActorCategoryResponseDTOToSecondaryActorCategory(SecondaryActorCategoryDTO secondaryActorCategoryRequestDTO);
}
