package priam.consent.priamconsentservice.openfeign;

import java.util.Collection;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import priam.consent.priamconsentservice.entities.Processing;

@FeignClient(name = "DATA-SERVICE")
public interface DataRestClient {

    @GetMapping("/api/processing/listProcessings/{dsc}")
    Collection<Processing> getProcessingsByDataSubjectCategoryId(@PathVariable int dsc);

    @GetMapping("/api/processing/{id}")
    Processing getProcessing(@PathVariable int id);
}
