package priam.data.priamdataservice.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import priam.data.priamdataservice.services.ProcessedDataService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProcessedDataController {

    private final ProcessedDataService processedDataService;

    public ProcessedDataController(ProcessedDataService processedDataService) {
        this.processedDataService = processedDataService;
    }

    // 1. Ajouter des entrées dans la table processed_data
    @PostMapping("/processed-data/add")
    public ResponseEntity<String> addProcessedData(
            @RequestParam int subjectId,
            @RequestBody List<Integer> dataIds) {

        try {
            processedDataService.addProcessedData(dataIds, subjectId);
            return ResponseEntity.ok("Processed data added successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Supprimer des entrées dans la table processed_data
    @DeleteMapping("/processed-data/remove")
    public ResponseEntity<String> removeProcessedData(
            @RequestParam int subjectId,
            @RequestBody List<Integer> dataIds) {

        try {
            processedDataService.removeProcessedData(dataIds, subjectId);
            return ResponseEntity.ok("Processed data removed successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
