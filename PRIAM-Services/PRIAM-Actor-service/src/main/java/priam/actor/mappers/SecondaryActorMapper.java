package priam.actor.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import priam.actor.dto.SecondaryActorRequestDTO;
import priam.actor.dto.SecondaryActorResponseDTO;
import priam.actor.dto.SecondaryActorCategoryDTO;
import priam.actor.entities.SecondaryActor;
import priam.actor.entities.SecondaryActorCategory;
import priam.actor.entities.Country;

@Mapper(componentModel = "spring")
public interface SecondaryActorMapper {

    //@Mapping(target = "secondaryActorCategoryId", source = "secondaryActor.secondaryActorCategory.secondaryActorCategoryId")
    //@Mapping(target = "secondaryActorCategoryName", source = "secondaryActor.secondaryActorCategory.secondaryActorCategoryName")
    //@Mapping(target = "country", source = "secondaryActor.country")
    SecondaryActorResponseDTO SecondaryActorToSecondaryActorResponseDTO(SecondaryActor secondaryActor);

    @Mapping(target = "secondaryActorCategory.secondaryActorCategoryId", source = "secondaryActorRequestDTO.secondaryActorCategoryId")
    SecondaryActor SecondaryActorRequestDTOToSecondaryActor(SecondaryActorRequestDTO secondaryActorRequestDTO);
}