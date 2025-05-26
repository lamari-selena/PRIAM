package priam.consent.priamconsentservice.openfeign;

import java.util.Collection;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import priam.consent.priamconsentservice.entities.Processing;

@FeignClient(name = "gateway", contextId = "dataClient")
//@FeignClient(name = "DATA-SERVICE")
public interface DataRestClient {

    //{/data} prefix
    @GetMapping("/data/api/processing/listProcessings/{dsc}")
    Collection<Processing> getProcessingsByDataSubjectCategoryId(@PathVariable int dsc);

    @GetMapping("/data/api/processing/{id}")
    Processing getProcessing(@PathVariable String id);

    @PostMapping("/data/api/processed-data/add")
    public ResponseEntity<String> addProcessedData(
            @RequestParam int subjectId, @RequestBody List<Integer> dataIds);

    @DeleteMapping("/data/api/processed-data/remove")
    public ResponseEntity<String> removeProcessedData(
            @RequestParam int subjectId, @RequestBody List<Integer> dataIds);

    @GetMapping("/data/processing/data-usage/DataIds/{processingId}")
    public List<Integer> getDataIds(@PathVariable String processingId);
}