package priam.actor.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.actor.entities.SafeguardType;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class SecondaryActorDTO {
    private int secondaryActorId;
    private String secondaryActorName;
    private String secondaryActorEmail;
    private String secondaryActorPhone;
    private String secondaryActorAddress; //TODO: change to Address
    private String country; //TODO: change to Country
    private String safeguard;
    private SafeguardType safeguardType;

    private String username;
    private String password;

    private SecondaryActorCategoryDTO secondaryActorCategory;
}
