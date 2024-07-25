package priam.consent.priamconsentservice.mappers;

import org.mapstruct.Mapper;
import priam.consent.priamconsentservice.dto.ContractRequestDTO;
import priam.consent.priamconsentservice.dto.ContractResponseDTO;
import priam.consent.priamconsentservice.entities.Contract;

@Mapper(componentModel = "spring")
public interface ContractMapper {
    Contract fromContractRequestDTO(ContractRequestDTO dataRequestRequestDTO);

    ContractResponseDTO fromContractRequest(Contract contract);
}
