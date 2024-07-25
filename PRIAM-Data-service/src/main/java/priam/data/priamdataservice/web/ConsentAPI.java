package priam.data.priamdataservice.web;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import priam.data.priamdataservice.dto.consent.ConsentRequestDTO;
import priam.data.priamdataservice.dto.consent.ConsentResponseDTO;
import priam.data.priamdataservice.services.ConsentServiceInterface;

@RestController
@RequestMapping("/api/processing/consent")
@AllArgsConstructor
public class ConsentAPI {
    private final ConsentServiceInterface consentServiceInterface;

    /**
     * Save a new Consend
     * @param consentRequestDTO Information of the Consent
     * @return The created Consent object
     */
    @PostMapping("")
    public ConsentResponseDTO createConsent(@RequestBody ConsentRequestDTO consentRequestDTO) {
        return consentServiceInterface.save(consentRequestDTO);
    }
}
