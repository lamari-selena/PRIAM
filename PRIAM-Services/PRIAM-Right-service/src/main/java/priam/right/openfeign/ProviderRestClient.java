package priam.right.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import priam.right.dto.ErasureRequestDTO;
import priam.right.dto.RectificationRequestDTO;

import java.util.List;
import java.util.Map;

@FeignClient(name = "gateway", contextId = "providerClient")//@FeignClient(name = "PROVIDER-SERVICE")
public interface ProviderRestClient {

    //{/provider}  prefixe
    @PostMapping(path = "/provider/api/rectification")
    void rectification(@RequestBody RectificationRequestDTO rectificationRequestDTO);

    @PostMapping(path = "/provider/api/erasure")
    void erasure(@RequestBody ErasureRequestDTO erasureRequestDTO);

    // attributes is comma-joined by the caller into a single query param
    // (not repeated "attributes=a&attributes=b"): OpenFeign's default
    // List<String> query encoding sends repeated params, but the target
    // application's Provider bridge (e.g. FastAPI-Healthcare-PRIAM's
    // app/priam/router.py, a `str` Query param) only keeps the last one it
    // receives, silently dropping every attribute but the last requested.
    // Still supports any number of attributes — see DataRequestServiceImpl
    // .DataAccess, which joins the caller's List<String> before this call.
    // idRef must be the target app's external identifier (String), not
    // PRIAM's internal numeric data_subject_id — see
    // DataRequestServiceImpl.DataAccess, which resolves the numeric id to
    // idRef via ActorRestClient before calling this. Previously typed as
    // `int`, which only "worked" for case studies whose idRef happened to
    // be an integer-parseable string coinciding with dataSubjectId; a
    // non-numeric idRef (e.g. Ghostfolio-PRIAM-test1's "priam-demo-user")
    // exposed this — the wrong internal id was silently sent as idRef to
    // the target app's Provider bridge, returning no personal data.
    @GetMapping(path = "/provider/api/dataAccessRight")
    List<Map<String, String>> getPersonalDataValues(@RequestParam String idRef, @RequestParam String dataTypeName, @RequestParam String attributes);
}
