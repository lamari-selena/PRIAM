package priam.data.priamdataservice.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import priam.data.priamdataservice.dto.transfer.*;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.PersonalDataTransfer;
import priam.data.priamdataservice.entities.SecondaryActor;
import priam.data.priamdataservice.entities.SecondaryActorCategory;
import priam.data.priamdataservice.mappers.PersonalDataTransferMapper;
import priam.data.priamdataservice.repositories.DataRepository;
import priam.data.priamdataservice.repositories.transfer.PersonalDataTransferRepository;
import priam.data.priamdataservice.repositories.transfer.SecondaryActorCategoryRepository;
import priam.data.priamdataservice.repositories.transfer.SecondaryActorRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;

@Service
@Transactional
@AllArgsConstructor
public class TransferService implements TransferServiceInterface {

    private PersonalDataTransferRepository transferRepository;
    private SecondaryActorRepository secondaryActorRepository;
    private SecondaryActorCategoryRepository secondaryActorCategoryRepository;
    private DataRepository dataRepository;
    private PersonalDataTransferMapper mapper;

    @Override
    public PersonalDataTransferDTO createTransfer(PersonalDataTransferDTO transferDTO) {
        PersonalDataTransfer transfer = mapper.TransferDTOToTransfer(transferDTO);
        PersonalDataTransfer res = transferRepository.save(transfer);
        return mapper.TransferToTransferDTO(res);
    }

    @Override
    public SecondaryActorDTO createSecondaryActor(SecondaryActorDTO secondaryActorDTO) {
        SecondaryActor secondaryActor = mapper.SecondaryActorDTOToSecondaryActor(secondaryActorDTO);
        SecondaryActor res = secondaryActorRepository.save(secondaryActor);
        return mapper.SecondaryActorToSecondaryActorDTO(res);
    }

    @Override
    public SecondaryActorCategoryDTO createSecondaryActorCategory(SecondaryActorCategoryDTO secondaryActorCategoryDTO) {
        SecondaryActorCategory category = mapper.SecondaryActorCategoryDTOToSecondaryActorCategory(secondaryActorCategoryDTO);
        SecondaryActorCategory res = secondaryActorCategoryRepository.save(category);
        return mapper.SecondaryActorCategoryToSecondaryActorCategoryDTO(res);
    }

    @Override
    public void createSecondaryActorTransfer(SecondaryActorTransferDTO secondaryActorTransferDTO) {
        PersonalDataTransfer transfer = transferRepository.findPersonalDataTransferByPersonalDataTransferId(secondaryActorTransferDTO.getTransferId());
        ArrayList<SecondaryActor> list = new ArrayList<>(transfer.getSecondaryActors());
        SecondaryActor secondaryActor = secondaryActorRepository.findSecondaryActorBySecondaryActorId(secondaryActorTransferDTO.getSecondaryActorId());
        list.add(secondaryActor);
        transfer.setSecondaryActors(list);
        transferRepository.save(transfer);
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
}
