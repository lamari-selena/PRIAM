package priam.data.priamdataservice.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import priam.data.priamdataservice.dto.ropa.DpiaEntryDTO;
import priam.data.priamdataservice.dto.ropa.RopaEntryDTO;
import priam.data.priamdataservice.services.RopaService;

import java.util.List;

@RestController
@RequestMapping(path = "/api", produces = "application/json")
public class RopaController {

    private final RopaService ropaService;

    public RopaController(RopaService ropaService) {
        this.ropaService = ropaService;
    }

    /**
     * Returns the full Record of Processing Activities (ROPA) — GDPR Art. 30.
     */
    @GetMapping("/ropa")
    public List<RopaEntryDTO> getRopa() {
        return ropaService.generateRopa();
    }

    /**
     * Returns DPIA entries for high-risk processing activities — GDPR Art. 35.
     */
    @GetMapping("/dpia")
    public List<DpiaEntryDTO> getDpia() {
        return ropaService.generateDpia();
    }
}
