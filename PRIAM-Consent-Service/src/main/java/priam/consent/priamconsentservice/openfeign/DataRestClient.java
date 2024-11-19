package priam.consent.priamconsentservice.openfeign;

import java.util.Collection;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import priam.consent.priamconsentservice.entities.Processing;

@FeignClient(name = "DATA-SERVICE")
public interface DataRestClient {

    @GetMapping("/api/processing/listProcessings/{dsc}")
    Collection<Processing> getProcessingsByDataSubjectCategoryId(@PathVariable int dsc);

    @GetMapping("/api/processing/{id}")
    Processing getProcessing(@PathVariable String id);

    @PostMapping("/api/processed-data/add")
    public ResponseEntity<String> addProcessedData(
            @RequestParam int subjectId, @RequestBody List<Integer> dataIds);

    @DeleteMapping("/api/processed-data/remove")
    public ResponseEntity<String> removeProcessedData(
            @RequestParam int subjectId, @RequestBody List<Integer> dataIds);

    @GetMapping("/processing/data-usage/DataIds/{processingId}")
    public List<Integer> getDataIds(@PathVariable String processingId);
}