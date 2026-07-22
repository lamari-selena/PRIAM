package priam.actor.entities;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
@AllArgsConstructor
@NoArgsConstructor
@Entity
@lombok.Data
@Table(name = "SecondaryActor")
public class SecondaryActor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "secondary_actor_id")
    private int secondaryActorId;
    @Enumerated(EnumType.STRING)
    private SecondaryActorType secondaryActorType;
    private String secondaryActorName;
    @ManyToOne
    @JoinColumn(name="address_id")
    private Address address; //TODO: Change type to Address
    private String secondaryActorPhone;
    private String secondaryActorEmail;
    private String safeguard;
    @Enumerated(EnumType.STRING)
    private SafeguardType safeguardType;

    //@ToString.Exclude
    @JsonManagedReference
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="country_id")
    private Country country;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "secondary_actor_category_id")
    @JsonManagedReference
    private SecondaryActorCategory secondaryActorCategory;
    //@ToString.Exclude
    /*@JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_actor_category_id")
    private SecondaryActorCategory secondaryActorCategory;*/
}
