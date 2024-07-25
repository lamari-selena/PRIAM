package priam.actor.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import priam.actor.entities.DataSubjectCategory;

public interface DataSubjectCategoryRepository extends JpaRepository<DataSubjectCategory, Integer> {
    DataSubjectCategory findDataSubjectCategoryByDataSubjectCategoryId(int dataSubjectCategoryId);
}
