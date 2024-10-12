package priam.data.priamdataservice.mappers;

import org.mapstruct.Mapper;
import priam.data.priamdataservice.dto.consent.ConsentRequestDTO;
import priam.data.priamdataservice.dto.consent.ConsentResponseDTO;
import priam.data.priamdataservice.entities.consent.Consent;

@Mapper(componentModel = "spring")
public interface ConsentMapper {
    Consent ConsentRequestDTOToConsent(ConsentRequestDTO consentRequestDTO);
    ConsentResponseDTO ConsentToConsentResponseDTO(Consent consent);
}
