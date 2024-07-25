package priam.data.priamdataservice.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.dto.DataResponseDTO;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonalDataTransferDTO {
    private int personalDataTransferId;
    private int processingId;
    private List<SecondaryActorDTO> secondaryActors = new ArrayList<>();
    private List<DataResponseDTO> data = new ArrayList<>();
}
