package priam.actor.services;

import org.springframework.stereotype.Service;
import priam.actor.dto.DataSubjectCategoryRequestDTO;
import priam.actor.dto.DataSubjectCategoryResponseDTO;
import priam.actor.dto.DataSubjectRequestDTO;
import priam.actor.dto.DataSubjectResponseDTO;
import priam.actor.entities.DataSubjectCategory;
import priam.actor.entities.DataSubject;
import priam.actor.mappers.DataSubjectCategoryMapper;
import priam.actor.mappers.DataSubjectMapper;
import priam.actor.repositories.DataSubjectCategoryRepository;
import priam.actor.repositories.DataSubjectRepository;

import javax.annotation.Generated;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        date = "2021-05-23T23:03:41+0530"
)
@Service
@Transactional
public class DataSubjectServiceImpl implements DataSubjectService {
    final DataSubjectRepository dataSubjectRepository;
    final DataSubjectCategoryRepository dataSubjectCategoryRepository;
    final DataSubjectMapper dataSubjectMapper;

    final DataSubjectCategoryMapper dataSubjectCategoryMapper;

    public DataSubjectServiceImpl(DataSubjectRepository dataSubjectRepository, DataSubjectCategoryRepository dataSubjectCategoryRepository, DataSubjectMapper dataSubjectMapper, DataSubjectCategoryMapper dataSubjectCategoryMapper) {
        this.dataSubjectRepository = dataSubjectRepository;
        this.dataSubjectCategoryRepository = dataSubjectCategoryRepository;
        this.dataSubjectMapper = dataSubjectMapper;
        this.dataSubjectCategoryMapper = dataSubjectCategoryMapper;
    }

    @Override
    public DataSubjectResponseDTO saveDataSubject(DataSubjectRequestDTO dataSubjectRequestDTO) {
        DataSubject dataSubject = dataSubjectMapper.DataSubjectRequestDTOToDataSubject(dataSubjectRequestDTO);
        DataSubject result = dataSubjectRepository.save(dataSubject);
        return dataSubjectMapper.DataSubjectToDataSubjectResponseDTO(result);
    }
    @Override
    public DataSubjectResponseDTO findDataSubject(int dataSubjectId) {
        DataSubject dataSubject = dataSubjectRepository.findById(dataSubjectId).get();
        DataSubjectResponseDTO dataSubjectResponseDTO =
                dataSubjectMapper.DataSubjectToDataSubjectResponseDTO(dataSubject);
        return dataSubjectResponseDTO;
    }

    @Override
    public DataSubjectResponseDTO getDataSubjectByIdRef(String idRef) {
        DataSubject dataSubject = dataSubjectRepository.findDataSubjectByIdRef(idRef);
        DataSubjectResponseDTO dataSubjectResponseDTO =
                dataSubjectMapper.DataSubjectToDataSubjectResponseDTO(dataSubject);
        return dataSubjectResponseDTO;
    }

    @Override
    public int getDataSubjectIdByIdRef(String idRef) {
        DataSubject dataSubject = dataSubjectRepository.findDataSubjectByIdRef(idRef);
        return dataSubject.getDataSubjectId();
    }

    @Override
    public DataSubjectCategoryResponseDTO saveDataSubjectCategory(DataSubjectCategoryRequestDTO dataSubjectCategoryRequestDTO) {
        DataSubjectCategory dataSubjectCategory = dataSubjectCategoryMapper.DataSubjectCategoryResponseDTOToDataSubjectCategory(dataSubjectCategoryRequestDTO);
        DataSubjectCategory result = dataSubjectCategoryRepository.save(dataSubjectCategory);
        return dataSubjectCategoryMapper.DataSubjectCategoryToDataSubjectCategoryResponseDTO(result);
    }

    @Override
    public List<DataSubjectCategoryResponseDTO> getAllDataSubjectCategories() {
        List<DataSubjectCategory> result = dataSubjectCategoryRepository.findAll();
        List<DataSubjectCategoryResponseDTO> response = new ArrayList<>();
        result.forEach(dsc -> {
            response.add(dataSubjectCategoryMapper.DataSubjectCategoryToDataSubjectCategoryResponseDTO(dsc));
        });
        return response;
    }

    @Override
    public DataSubjectCategoryResponseDTO getDataSubjectCategoryById(int dataSubjectCategoryId) {
        DataSubjectCategory dataSubjectCategory = dataSubjectCategoryRepository.findDataSubjectCategoryByDataSubjectCategoryId(dataSubjectCategoryId);
        DataSubjectCategoryResponseDTO dataSubjectCategoryResponseDTO =
                dataSubjectCategoryMapper.DataSubjectCategoryToDataSubjectCategoryResponseDTO(dataSubjectCategory);
        return dataSubjectCategoryResponseDTO;
    }

}
