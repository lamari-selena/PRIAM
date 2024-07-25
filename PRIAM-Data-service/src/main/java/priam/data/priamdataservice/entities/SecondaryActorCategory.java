package priam.data.priamdataservice.entities;

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
}
