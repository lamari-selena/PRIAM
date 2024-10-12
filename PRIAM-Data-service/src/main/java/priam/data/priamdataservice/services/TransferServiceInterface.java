package priam.data.priamdataservice.services;

import priam.data.priamdataservice.dto.transfer.*;

public interface TransferServiceInterface {
    /**
     * Save a new Transfer
     * @param transferDTO Information of the Transfer
     * @return The created Transfer object
     */
    PersonalDataTransferDTO createTransfer(PersonalDataTransferDTO transferDTO);
    /**
     * Save a new SecondaryActor
     * @param secondaryActorDTO Information of the SecondaryActor
     * @return The created SecondaryActor object
     */
    SecondaryActorDTO createSecondaryActor(SecondaryActorDTO secondaryActorDTO);
    /**
     * Save a new SecondaryActorCategory
     * @param secondaryActorCategoryDTO Information of the SecondaryActorCategory
     * @return The created SecondaryActorCategory object
     */
    SecondaryActorCategoryDTO createSecondaryActorCategory(SecondaryActorCategoryDTO secondaryActorCategoryDTO);
    /**
     * Save a new SecondaryActorTransfer
     * @param secondaryActorTransferDTO Information of the SecondaryActorTransfer
     */
    void createSecondaryActorTransfer(SecondaryActorTransferDTO secondaryActorTransferDTO);
    /**
     * Save a new DataTransfer
     * @param dataTransferDTO Information of the DataTransfer
     */
    void createDataTransfer(DataTransferDTO dataTransferDTO);
}
