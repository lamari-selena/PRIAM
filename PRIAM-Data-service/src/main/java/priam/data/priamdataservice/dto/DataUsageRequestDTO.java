package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.Processing;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class DataUsageRequestDTO {
    private int dataUsageId;

    private boolean personalStatus;

    private boolean c;

    private boolean r;

    private boolean u;

    private boolean d;

    private Processing processing;

    private int dataId;

    private Data data;

}
