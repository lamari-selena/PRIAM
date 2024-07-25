package priam.consent.priamconsentservice.web;

import org.springframework.web.bind.annotation.*;
import priam.consent.priamconsentservice.dto.ConsentRequestDTO;
import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.entities.Processing;
import priam.consent.priamconsentservice.services.ConsentService;

import java.util.Collection;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = "/api/consent", produces = "application/json")
public class ConsentRestController {

    private ConsentService consentService;

    public ConsentRestController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/{id}")
    public ConsentResponseDTO getConsent(@PathVariable int id) {
        return consentService.getConsent(id);
    }

    @PostMapping("/create")
    public ConsentResponseDTO newConsent(ConsentRequestDTO consentRequestDTO) {
        return consentService.create(consentRequestDTO);
    }

    @GetMapping("/listProcessings/{dsc}")
    public Collection<Processing> getProcessingListByDsc(@PathVariable int dsc) {
        return consentService.getProcessingListByDsc(dsc);
    }
}

    /*@PutMapping("/update/{processingId}")
    public ProcessingResponseDTO modifyProcessing(@PathVariable Long processingId, @RequestBody ProcessingRequestDTO processingRequestDTO) {
        return processingService.updateProcessing(processingId,processingRequestDTO);
    }
}*/
