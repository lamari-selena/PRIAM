package org.provider_microservice.mappers;

import org.mapstruct.Mapper;
import org.provider_microservice.dto.ProviderViewRequestDTO;
import org.provider_microservice.dto.ProviderViewResponseDTO;
import org.provider_microservice.entities.ProviderView;

@Mapper(componentModel = "spring")
public interface ProviderViewMapper {
    ProviderViewResponseDTO VueNutritionToVueNutritionResponseDTO(ProviderView vueNutrition);

    ProviderView VueNutritionRequestDTOToVueNutrition(ProviderViewRequestDTO vueNutritionRequestDTO);
}
