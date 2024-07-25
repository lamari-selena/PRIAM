package priam.right.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import priam.right.dto.ErasureRequestDTO;
import priam.right.dto.RectificationRequestDTO;

import java.util.List;
import java.util.Map;

@FeignClient(name = "PROVIDER-SERVICE")
public interface ProviderRestClient {

    // solution avec passage de paramétre par url
    //@PostMapping(path = "/api/rectification/{patientId}/{attribute}/{newValue}")
    //void rectification/*Data*/(@PathVariable String attribute, @PathVariable String newValue, @PathVariable int patientId);

    @PostMapping(path = "/api/rectification")
    void rectification(@RequestBody RectificationRequestDTO rectificationRequestDTO);

    @PostMapping(path = "/api/forgotten")
    void erasure(@RequestBody ErasureRequestDTO erasureRequestDTO);

    @GetMapping(path = "/api/dataAccessRight")
    List<Map<String, String>> getPersonalDataValues(@RequestParam int dataSubjectId, @RequestParam String dataTypeName, @RequestParam List<String> attributes);

    //@PostMapping(path = "/api/rectification/{patientId}/{attribute}")
    //void eraseData(@PathVariable String attribute, @PathVariable int patientId);

   /* @GetMapping(path = "/api/personalDataValues/{idDS}/{dataTypeName}/{attributes}")
    List<Map<String, String>> PersonalDataValues(@PathVariable int idDS, @PathVariable String dataTypeName, @PathVariable List<String> attributes);*/


}
