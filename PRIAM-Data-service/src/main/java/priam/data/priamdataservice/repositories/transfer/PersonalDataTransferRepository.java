package priam.data.priamdataservice.repositories.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import priam.data.priamdataservice.entities.PersonalDataTransfer;

@Repository
public interface PersonalDataTransferRepository extends JpaRepository<PersonalDataTransfer, Integer> {
    PersonalDataTransfer findPersonalDataTransferByPersonalDataTransferId(int transferId);
}
