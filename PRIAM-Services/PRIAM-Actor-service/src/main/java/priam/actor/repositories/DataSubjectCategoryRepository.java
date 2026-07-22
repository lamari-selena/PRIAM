package priam.actor.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import priam.actor.entities.DataSubjectCategory;

import java.util.ArrayList;
import java.util.List;

public interface DataSubjectCategoryRepository extends JpaRepository<DataSubjectCategory, Integer> {

    DataSubjectCategory findDataSubjectCategoryByDataSubjectCategoryId(int dataSubjectCategoryId);

    List<DataSubjectCategory> findAll();
}
