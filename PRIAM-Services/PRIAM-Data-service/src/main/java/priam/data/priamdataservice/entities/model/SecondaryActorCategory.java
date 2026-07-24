package priam.data.priamdataservice.entities.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class SecondaryActorCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int secondaryActorCategoryId;

    private String secondaryActorCategoryName;
}
