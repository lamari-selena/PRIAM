package priam.data.priamdataservice.web;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import priam.data.priamdataservice.dto.transfer.*;
import priam.data.priamdataservice.services.TransferService;

@RestController
@RequestMapping("transfer")
@AllArgsConstructor
public class TransferAPI {
    private final TransferService transferService;

    /**
     * Save a new Transfer
     * @param transferDTO Information of the Transfer
     * @return The created Transfer object
     */
    @PostMapping("")
    public PersonalDataTransferDTO createTransfer(@RequestBody PersonalDataTransferDTO transferDTO) {
        return transferService.createTransfer(transferDTO);
    }

    /**
     * Save a new SecondaryActor
     * @param secondaryActorDTO Information of the SecondaryActor
     * @return The created SecondaryActor object
     */
    @PostMapping("/secondaryActor")
    public SecondaryActorDTO createSecondaryActor(@RequestBody SecondaryActorDTO secondaryActorDTO) {
        return transferService.createSecondaryActor(secondaryActorDTO);
    }

    /**
     * Save a new SecondaryActorCategory
     * @param secondaryActorCategoryDTO Information of the SecondaryActorCategory
     * @return The created SecondaryActorCategory object
     */
    @PostMapping("/secondaryActorCategory")
    public SecondaryActorCategoryDTO createSecondaryActorCategory(@RequestBody SecondaryActorCategoryDTO secondaryActorCategoryDTO) {
        return transferService.createSecondaryActorCategory(secondaryActorCategoryDTO);
    }

    /**
     * Save a new SecondaryActorTransfer
     * @param secondaryActorTransferDTO Information of the SecondaryActorTransfer
     */
    @PostMapping("/secondaryActorTransfer")
    public void createSecondaryActorTransfer(@RequestBody SecondaryActorTransferDTO secondaryActorTransferDTO) {
        transferService.createSecondaryActorTransfer(secondaryActorTransferDTO);
    }

    /**
     * Save a new DataTransfer
     * @param dataTransferDTO Information of the DataTransfer
     */
    @PostMapping("/dataTransfer")
    public void createDataTransfer(@RequestBody DataTransferDTO dataTransferDTO) {
        transferService.createDataTransfer(dataTransferDTO);
    }
}
