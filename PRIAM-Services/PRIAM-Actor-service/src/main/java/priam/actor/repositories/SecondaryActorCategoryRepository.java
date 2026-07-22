package priam.actor.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import priam.actor.entities.SecondaryActor;
import priam.actor.entities.SecondaryActorCategory;

public interface SecondaryActorCategoryRepository extends JpaRepository<SecondaryActorCategory, Integer> {
}
