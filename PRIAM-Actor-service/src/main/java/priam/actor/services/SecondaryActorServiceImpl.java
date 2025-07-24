package priam.actor.services;

import org.springframework.stereotype.Service;
import priam.actor.entities.SecondaryActor;
import priam.actor.mappers.SecondaryActorMapper;
import priam.actor.repositories.SecondaryActorCategoryRepository;
import priam.actor.repositories.SecondaryActorRepository;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class SecondaryActorServiceImpl implements SecondaryActorService {
    private final SecondaryActorRepository secondaryActorRepository;
    private final SecondaryActorMapper secondaryActorMapper;
    private final SecondaryActorCategoryRepository secondaryActorCategoryRepository;

    public SecondaryActorServiceImpl(SecondaryActorCategoryRepository secondaryActorCategoryRepository, SecondaryActorRepository secondaryActorRepository, SecondaryActorMapper secondaryActorMapper) {
        this.secondaryActorRepository = secondaryActorRepository;
        this.secondaryActorCategoryRepository = secondaryActorCategoryRepository;
        this.secondaryActorMapper = secondaryActorMapper;
    }

    @Override
    public SecondaryActorResponseDTO saveSecondaryActor(SecondaryActorRequestDTO secondaryActorRequestDTO) {
        SecondaryActor secondaryActor = secondaryActorMapper.SecondaryActorRequestDTOToSecondaryActor(secondaryActorRequestDTO);
        SecondaryActor result = secondaryActorRepository.save(secondaryActor);
        return secondaryActorMapper.SecondaryActorToSecondaryActorResponseDTO(result);
    }

    @Override
    public SecondaryActorResponseDTO findSecondaryActor(int secondaryActorId) {
        SecondaryActor secondaryActor = secondaryActorRepository.findById(secondaryActorId)
                .orElseThrow(() -> new EntityNotFoundException("SecondaryActor not found with id: " + secondaryActorId));
        return secondaryActorMapper.SecondaryActorToSecondaryActorResponseDTO(secondaryActor);
    }

    @Override
    public List<SecondaryActor> getSecondaryActorsByDataId (int dataId){
        return secondaryActorRepository.findSecondaryActorsByDataId(dataId);
    }
}
