package priam.data.priamdataservice.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.entities.Data;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class DataTransferDTO {
    private int transferId;
    private int dataId;
    private Data data;
}
