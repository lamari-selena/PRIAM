package priam.data.priamdataservice.repositories.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import priam.data.priamdataservice.entities.SecondaryActor;

import java.util.List;

public interface SecondaryActorRepository extends JpaRepository<SecondaryActor, Integer> {
    SecondaryActor findSecondaryActorBySecondaryActorId(int secondaryActorId);

    @Query(value = "SELECT sa.secondary_actor_id, sa.secondary_actor_type, " +
            "sa.secondary_actor_name, sa.secondary_actor_phone, sa.secondary_actor_email, " +
            "sa.secondary_actor_address, sa.country_country_id, " +
            "c.country_name, sa.safeguard, sa.safeguard_type, " +
            "sa.secondary_actor_category_secondary_actor_category_id, " +
            "sac.secondary_actor_category_name " +
            "FROM secondary_actor sa " +
            "INNER JOIN personal_data_transfer_secondary_actor sat ON sa.secondary_actor_id = sat.secondary_actor_id " +
            "INNER JOIN personal_data_transfer pdt ON sat.personal_data_transfer_id = pdt.personal_data_transfer_id " +
            "INNER JOIN personal_data_transfer_data dt ON pdt.personal_data_transfer_id = dt.personal_data_transfer_id " +
            "INNER JOIN secondary_actor_category sac ON sa.secondary_actor_category_secondary_actor_category_id = sac.secondary_actor_category_id " +
            "INNER JOIN country c ON sa.country_country_id = c.country_id " +
            "WHERE dt.data_id = :dataId",
            nativeQuery = true)
    List<SecondaryActor> findSecondaryActorsByDataId(int dataId);
}
