package priam.data.priamdataservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import priam.data.priamdataservice.dto.DataUsageResponseDTO;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.DataUsage;
import priam.data.priamdataservice.mappers.DataUsageMapper;
import priam.data.priamdataservice.repositories.DataRepository;
import priam.data.priamdataservice.repositories.DataUsageRepository;

import javax.annotation.Generated;
import javax.transaction.Transactional;
import java.util.Collection;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        date = "2021-05-23T23:03:41+0530"
)

@Slf4j
@Transactional
@Service
public class DataUsageService implements DataUsageServiceInterface {

    private DataUsageRepository dataUsageRepository;
    private DataRepository dataRepository;
    private DataUsageMapper dataUsageMapper;

    public DataUsageService(DataUsageMapper dataUsageMapper, DataUsageRepository dataUsageRepository, DataRepository dataRepository) {
        this.dataUsageMapper = dataUsageMapper;
        this.dataUsageRepository = dataUsageRepository;
        this.dataRepository = dataRepository;
    }
    @Override
    public DataUsage createDataUsage(DataUsage dataUsage) {
        //log.info("CreateDataUsage start Process !");
        DataUsage res = dataUsageRepository.save(dataUsage);
        //log.info("CreateDataUsage end Process !");
        return res;
    }

    @Override
    public DataUsage updateDataUsage(DataUsage dataUsage) {
        //log.info("UpdateDataUsage start Process !");
        DataUsage res = dataUsageRepository.save(dataUsage);
        //log.info("UpdateDataUsage end Process !");
        return res;
    }

    @Override
    public boolean deleteDataUsage(Integer dataUsageId) {
        dataUsageRepository.deleteById(dataUsageId);
        return true;
    }

    @Override
    public DataUsageResponseDTO getDataUsage(Integer dataUsageId) {
        DataUsage dataUsage = dataUsageRepository.findById(dataUsageId).get();
        Data data = dataRepository.findByDataId(dataUsage.getDataId()).get();
        dataUsage.setData(data);
        return dataUsageMapper.fromDataUsage(dataUsage);/*.orElseThrow();*/
    }

    @Override
    public Collection<DataUsage> getDataUsages(int processingId) {
        //Collection<DataUsage> datausages = dataUsageRepository.findAll();
        Collection<DataUsage> datausages = dataUsageRepository.findAllByProcessing_ProcessingId(processingId);
        for (DataUsage d: datausages){
            Data data = dataRepository.findByDataId(d.getDataId()).get();
            d.setData(data);
        }
        return datausages;
    }
}
