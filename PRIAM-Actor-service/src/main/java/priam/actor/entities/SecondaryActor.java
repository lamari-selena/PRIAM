package priam.actor.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@lombok.Data
@Table(name = "SecondaryActor")
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
    private Country country;

    @JsonBackReference
    @ManyToOne
    private SecondaryActorCategory secondaryActorCategory;
}
