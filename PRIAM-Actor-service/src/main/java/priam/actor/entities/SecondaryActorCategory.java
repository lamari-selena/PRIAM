package priam.actor.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Collection;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@lombok.Data
public class SecondaryActorCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int secondaryActorCategoryId;

    private String secondaryActorCategoryName;

    @JsonManagedReference
    @OneToMany(mappedBy ="secondaryActorCategory", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Collection<SecondaryActor> secondaryActors;
}
