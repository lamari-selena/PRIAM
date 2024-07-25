package priam.actor.web;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import priam.actor.dto.DataSubjectCategoryRequestDTO;
import priam.actor.dto.DataSubjectCategoryResponseDTO;
import priam.actor.dto.DataSubjectRequestDTO;
import priam.actor.dto.DataSubjectResponseDTO;
import priam.actor.mappers.DataSubjectMapper;
import priam.actor.services.DataSubjectService;

@RestController
@RequestMapping(path = "/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DataSubjectRestAPI {

    private DataSubjectService dataSubjectService;
    private DataSubjectMapper dataSubjectMapper;

    public DataSubjectRestAPI(DataSubjectService dataSubjectService) {
        this.dataSubjectService = dataSubjectService;
    }

    /**
     * Save a new data subject
     *
     * @param dataSubjectRequestDTO A DataSubjectRequestDTO object
     * @return A DataSubjectResponseDTO object
     */
    @PostMapping(path = "/DataSubject")
    public DataSubjectResponseDTO createDataSubject(@RequestBody DataSubjectRequestDTO dataSubjectRequestDTO) {
        return dataSubjectService.saveDataSubject(dataSubjectRequestDTO);
    }

    /**
     * Retrieve a DataSubjectResponseDTO object based on a data subject ID
     *
     * @param dataSubjectId The data subject ID
     * @return A DataSubjectResponseDTO object
     */
    @GetMapping(path = "/DataSubject/{dataSubjectId}")
    public DataSubjectResponseDTO getDataSubjectId(@PathVariable int dataSubjectId) {
        return dataSubjectService.findDataSubject(dataSubjectId);
    }

    @GetMapping(path = "/DataSubjectId/{idRef}")
    public int getDataSubjectIdByIdRef(@PathVariable String idRef) {
        return dataSubjectService.getDataSubjectIdByIdRef(idRef);
    }

    /**
     * Retrieve a DataSubjectResponseDTO based on a reference ID
     *
     * @param idRef The reference ID
     * @return A DataSubjectResponseDTO object
     */
    @GetMapping(path = "/DataSubject/ref/{idRef}")
    public DataSubjectResponseDTO getDataSubjectByRef(@PathVariable String idRef) {
        return dataSubjectService.getDataSubjectByIdRef(idRef);
    }

    /**
     * Save a new data subject category
     *
     * @param dataSubjectCategoryRequestDTO A DataSubjectCategoryRequestDTO object
     * @return A DataSubjectCategoryResponseDTO object
     */
    @PostMapping(path = "/actor/DataSubjectCategory")
    public DataSubjectCategoryResponseDTO createDataSubjectCategory(@RequestBody DataSubjectCategoryRequestDTO dataSubjectCategoryRequestDTO) {
        return dataSubjectService.saveDataSubjectCategory(dataSubjectCategoryRequestDTO);
    }

    /**
     * Retrieve all the data subject category in a list of DataSubjectCategoryResponseDTO objects
     * @return A list of DataSubjectCategoryResponseDTO objects
     */
    @GetMapping(path = "/actor/DataSubjectCategories")
    public List<DataSubjectCategoryResponseDTO> getAllDataSubjectCategories() {
        return dataSubjectService.getAllDataSubjectCategories();
    }

    /**
     * Retrieve a DataSubjectCategoryResponseDTO object based on a data subject category ID
     * @param dataSubjectCategoryId The data subject category ID
     * @return A DataSubjectCategoryResponseDTO object
     */
    @GetMapping(path = "/actor/DataSubjectCategory/{dataSubjectCategoryId}")
    public DataSubjectCategoryResponseDTO getDataSubjectCategoryById(@PathVariable int dataSubjectCategoryId) {
        return dataSubjectService.getDataSubjectCategoryById(dataSubjectCategoryId);
    }
}
