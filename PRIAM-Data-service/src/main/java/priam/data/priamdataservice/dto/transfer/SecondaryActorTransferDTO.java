package priam.data.priamdataservice.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.entities.SecondaryActor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecondaryActorTransferDTO {
    private int transferId;
    private int secondaryActorId;
    private SecondaryActor secondaryActor;
}
