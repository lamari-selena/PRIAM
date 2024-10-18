package priam.actor.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.actor.entities.Address;
import priam.actor.entities.SafeguardType;
@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class SecondaryActorRequestDTO {
    private int secondaryActorId;
    private String secondaryActorName;
    private String secondaryActorEmail;
    private String secondaryActorPhone;
    //private Address address; //TODO: change to Address
    //private String country; //TODO: change to Country
    private String safeguard;
    private SafeguardType safeguardType;
    private int secondaryActorCategoryId;
}
