package priam.data.priamdataservice.services;

import priam.data.priamdataservice.dto.transfer.PersonalDataTransferDTO;
import priam.data.priamdataservice.dto.transfer.SecondaryActorTransferDTO;
import priam.data.priamdataservice.dto.transfer.DataTransferDTO;

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

    /**
     * Save a new SecondaryActorTransfer
     * @param secondaryActorTransferDTO Information of the SecondaryActorTransfer
     */
    //void createSecondaryActorTransfer(SecondaryActorTransferDTO secondaryActorTransferDTO);
    /**
     * Save a new DataTransfer
     * @param dataTransferDTO Information of the DataTransfer
     */
    void createDataTransfer(DataTransferDTO dataTransferDTO);

    //public void createSecondaryActorTransfer(SecondaryActorTransferDTO secondaryActorTransferDTO);
}
