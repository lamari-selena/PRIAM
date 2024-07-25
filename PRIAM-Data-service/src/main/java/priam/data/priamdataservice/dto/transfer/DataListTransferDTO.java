package priam.data.priamdataservice.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.dto.ProcessedPersonalDataDTO;
import priam.data.priamdataservice.entities.Country;
import priam.data.priamdataservice.enums.SafeguardType;
import priam.data.priamdataservice.enums.SecondaryActorType;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class DataListTransferDTO { //TODO: check usage
    private int secondaryActorId;
    private SecondaryActorType secondaryActorType;
    private String secondaryActorName;
    private String secondaryActorEmail;
    private String secondaryActorPhone;
    private String secondaryActorAddress;
    private Country country;
    private String safeguard;
    private SafeguardType safeguardType;

    private SecondaryActorCategoryDTO secondaryActorCategory;

    private ProcessedPersonalDataDTO dataTransfers;
}
