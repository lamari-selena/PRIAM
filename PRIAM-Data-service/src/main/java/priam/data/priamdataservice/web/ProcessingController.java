/*package priam.data.priamdataservice.web;

import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import priam.data.priamdataservice.dto.ProcessingRequestDTO;
import priam.data.priamdataservice.dto.ProcessingResponseDTO;
import priam.data.priamdataservice.services.ProcessingService;
import priam.data.priamdataservice.entities.Processing;

import java.util.Collection;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = "/processing", produces = "application/json")
public class ProcessingController {

    private final ProcessingService processingService;

    @Autowired
    private KeycloakRestTemplate keycloakRestTemplate;

    public ProcessingController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    *//**
     * Save a new Processing
     * @param processingRequestDTO Information of the Processing
     * @return The created Processing object
     *//*
    @PostMapping("/create")
    public Processing newProcessing(@RequestBody ProcessingRequestDTO processingRequestDTO) {
        return processingService.createProcessing(processingRequestDTO);
    }

    *//**
     * Update a Processing object
     * @param processingId ID of the Processing
     * @param processingRequestDTO The new information of the Processing
     * @return The updated Processing object
     *//*
    @PutMapping("/update/{processingId}")
    public ProcessingResponseDTO modifyProcessing(@PathVariable Integer processingId, @RequestBody ProcessingRequestDTO processingRequestDTO) {
        return processingService.updateProcessing(processingId, processingRequestDTO);
    }

    *//**
     * Retrieve a Processing object by its ID
     * @param id ID of the Processing
     * @return A Processing object
     *//*
    @GetMapping("/{id}")
    public ProcessingResponseDTO getProcessing(@PathVariable Integer id) {
        ResponseEntity<ProcessingResponseDTO> response = keycloakRestTemplate.getForEntity("/processing/{id}", ProcessingResponseDTO.class, id);
        return response.getBody();
    }

    *//**
     * Retrieve all Processing
     * @return A Processing object List
     *//*
    @GetMapping("/processing/listProcessings")
    public ResponseEntity<Collection<Processing>> getProcessings() {
        ResponseEntity<Collection<Processing>> response = keycloakRestTemplate.exchange(
                "/processing/listProcessings", HttpMethod.GET,
                null, new ParameterizedTypeReference<Collection<Processing>>() {}
        );
        return response;
    }

    *//**
     * Retrieve a list of Processing object by the DataSubjectCategoryID
     * @param dataSubjectCategoryId ID of the DataSubjectCategory
     * @return A ProcessingResponseDTO object List
     *//*
    @GetMapping("/listProcessings/{dataSubjectCategoryId}")
    public Collection<ProcessingResponseDTO> getProcessingsByDataSubjectCategoryId(@PathVariable int dataSubjectCategoryId) {
        ResponseEntity<Collection<ProcessingResponseDTO>> response = keycloakRestTemplate.exchange(
                "/processing/listProcessings/{dataSubjectCategoryId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Collection<ProcessingResponseDTO>>() {},
                dataSubjectCategoryId
        );
        return response.getBody();
    }}*/


package priam.data.priamdataservice.web;

import java.util.Collection;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import priam.data.priamdataservice.dto.ProcessingRequestDTO;
import priam.data.priamdataservice.dto.ProcessingResponseDTO;
import priam.data.priamdataservice.entities.Processing;
import priam.data.priamdataservice.services.ProcessingService;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = "/api/processing", produces = "application/json")
public class ProcessingController {

    private final ProcessingService processingService;

    public ProcessingController(ProcessingService processingService) {
        this.processingService = processingService;
    }

/**
     * Save a new Processing
     * @param processingRequestDTO Information of the Processing
     * @return The created Processing object
     */


    @PostMapping("/create")
    public Processing newProcessing(@RequestBody ProcessingRequestDTO processingRequestDTO) {
        return processingService.createProcessing(processingRequestDTO);
    }



/**
     * Update a Processing object
     * @param processingId ID of the Processing
     * @param processingRequestDTO The new information of the Processing
     * @return The updated Processing object
     */


    @PutMapping("/update/{processingId}")
    public ProcessingResponseDTO modifyProcessing(@PathVariable Integer processingId, @RequestBody ProcessingRequestDTO processingRequestDTO) {
        return processingService.updateProcessing(processingId,processingRequestDTO);
    }

/**
     * Retrieve a Processing object by its ID
     * @param id ID of the Processing
     * @return A Processing object
     */


    @GetMapping("/{id}")
    public ProcessingResponseDTO getProcessing(@PathVariable Integer id) {
        return processingService.getProcessing(id);
    }



/**
     * Retrieve all Processing
     * @return A Processing object List
     */


    @GetMapping("/listProcessings")
    public Collection<Processing> getProcessings() {
        return processingService.getProcessings();
    }



/**
     * Retrieve a list of Processing object by the DataSubjectCategoryID
     * @param dataSubjectCategoryId ID of the DataSubjectCategory
     * @return A ProcessingResponseDTO object List
     */


    @GetMapping("/listProcessings/{dataSubjectCategoryId}")
    public Collection<ProcessingResponseDTO> getProcessingsByDataSubjectCategoryId(@PathVariable int dataSubjectCategoryId) {
        return processingService.getProcessingsByDataSubjectCategoryId(dataSubjectCategoryId);
    }
}
