package priam.actor.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "secondaryActorCategory", fetch = FetchType.LAZY)
    @JsonBackReference
    private Collection<SecondaryActor> secondaryActors;

    //@ToString.Exclude
/*    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "secondaryActorCategory",fetch = FetchType.LAZY)
    private Collection<SecondaryActor> secondaryActors;*/
}
