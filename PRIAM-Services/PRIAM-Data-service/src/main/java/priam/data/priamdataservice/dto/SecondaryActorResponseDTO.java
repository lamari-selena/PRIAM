package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import priam.data.priamdataservice.enums.SafeguardType;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class SecondaryActorResponseDTO {
    private int secondaryActorId;
    private String secondaryActorName;
    private String secondaryActorEmail;
    private String secondaryActorPhone;
    //private Address address; //TODO: change to Address
    //private Country country; //TODO: change to Country
    private String safeguard;
    private priam.data.priamdataservice.enums.SafeguardType safeguardType;

    private int secondaryActorCategoryId;
    //private SecondaryActorCategoryDTO secondaryActorCategory;
    private String secondaryActorCategoryName;
}
