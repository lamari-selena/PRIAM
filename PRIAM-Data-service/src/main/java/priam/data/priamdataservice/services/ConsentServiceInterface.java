package priam.data.priamdataservice.services;

import priam.data.priamdataservice.dto.consent.ConsentRequestDTO;
import priam.data.priamdataservice.dto.consent.ConsentResponseDTO;

public interface ConsentServiceInterface {

    /**
     * Save a new Consend
     * @param consentRequestDTO Information of the Consent
     * @return The created Consent object
     */
    ConsentResponseDTO save(ConsentRequestDTO consentRequestDTO);
}
