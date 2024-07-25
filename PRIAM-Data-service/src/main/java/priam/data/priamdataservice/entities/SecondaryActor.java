package priam.data.priamdataservice.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.enums.SafeguardType;
import priam.data.priamdataservice.enums.SecondaryActorType;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@lombok.Data
@Table(name = "secondary_actor")
public class SecondaryActor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int secondaryActorId;
    private SecondaryActorType secondaryActorType;
    private String secondaryActorName;
    private String secondaryActorAddress; //TODO: Change type to Address
    private String secondaryActorPhone;
    private String secondaryActorEmail;
    private String safeguard;
    private SafeguardType safeguardType;

    @ManyToOne
    @JoinColumn(name = "country_country_id")
    private Country country;

    @ManyToOne
    private SecondaryActorCategory secondaryActorCategory;
}
