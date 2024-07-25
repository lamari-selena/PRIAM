package priam.consent.priamconsentservice.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.dto.ContractResponseDTO;
import priam.consent.priamconsentservice.entities.Contract;
import priam.consent.priamconsentservice.services.ContractService;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = "/api", produces = "application/json")
public class ContractRestController {

    private ContractService contractService;

    @Autowired
    public ContractRestController (ContractService contractService){
        this.contractService = contractService;
    }

    @GetMapping("/contract/{idDataSubject}")
    public ContractResponseDTO getContractByDataSubject(@PathVariable int idDataSubject) {
        return contractService.getContractByIdDataSubject(idDataSubject);
    }

    //Consent Information Point (CIP)
    @GetMapping("/contract/list/consents/{idRefDataSubject}/{idProcessing}")
    public List<ConsentResponseDTO> getListConsentByDataSubject(@PathVariable String idRefDataSubject,@PathVariable int idProcessing) {
        return contractService.getListConsentByDataSubject(idRefDataSubject, idProcessing);
    }



    //Consent Information Point (CDP)
    @GetMapping("/decision/{processingId}")
    public Map<String, Boolean>  getDecisionByDataSubject (@RequestParam List<String> idRefList, @PathVariable int processingId) {
        return contractService.ConsentDecesionPoint(idRefList, processingId);
    }
}
