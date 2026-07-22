package priam.data.priamdataservice.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import priam.data.priamdataservice.dto.transfer.DataTransferDTO;
import priam.data.priamdataservice.dto.transfer.PersonalDataTransferDTO;
import priam.data.priamdataservice.dto.transfer.SecondaryActorTransferDTO;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.PersonalDataTransfer;
import priam.data.priamdataservice.mappers.PersonalDataTransferMapper;
import priam.data.priamdataservice.openfeign.ActorRestClient;
import priam.data.priamdataservice.repositories.DataRepository;
import priam.data.priamdataservice.repositories.transfer.PersonalDataTransferRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;

@Service
@Transactional
@AllArgsConstructor
public class TransferService implements TransferServiceInterface {

    private PersonalDataTransferRepository transferRepository;
    private ActorRestClient actorRestClient;
    private DataRepository dataRepository;
    private PersonalDataTransferMapper mapper;

    @Override
    public PersonalDataTransferDTO createTransfer(PersonalDataTransferDTO transferDTO) {
        PersonalDataTransfer transfer = mapper.TransferDTOToTransfer(transferDTO);
        PersonalDataTransfer res = transferRepository.save(transfer);
        return mapper.TransferToTransferDTO(res);
    }


    @Override
    public void createDataTransfer(DataTransferDTO dataTransferDTO) {
        PersonalDataTransfer transfer = transferRepository.findPersonalDataTransferByPersonalDataTransferId(dataTransferDTO.getTransferId());
        ArrayList<Data> list = new ArrayList<>(transfer.getData());
        Data data = dataRepository.findByDataId(dataTransferDTO.getDataId()).get();
        list.add(data);
        transfer.setData(list);
        transferRepository.save(transfer);
    }

    /*@Override
    public void createSecondaryActorTransfer(SecondaryActorTransferDTO secondaryActorTransferDTO) {
        PersonalDataTransfer transfer = transferRepository.findPersonalDataTransferByPersonalDataTransferId(secondaryActorTransferDTO.getTransferId());
        ArrayList<SecondaryActor> list = new ArrayList<>(transfer.getSecondaryActors());
        SecondaryActor secondaryActor = actorRestClient.findSecondaryActorBySecondaryActorId(secondaryActorTransferDTO.getSecondaryActorId());
        list.add(secondaryActor);
        transfer.setSecondaryActors(list);
        transferRepository.save(transfer);
    }
*/

}
