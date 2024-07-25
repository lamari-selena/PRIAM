package priam.right.openfeign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import priam.right.entities.Data;

@FeignClient(name = "DATA-SERVICE")
//@FeignClient(name ="DATA-SERVICE")
public interface DataRestClient {

    @GetMapping(path = "/api/dataId/{dataName}")
    public int getDataIdByName(@PathVariable String dataName);

    @GetMapping(path = "/api/datatype/data/{dataTypeId}")
    public String getDataTypeNameByDataTypeId(@PathVariable int dataTypeId);

    @GetMapping(path = "/api/personalData/{dataId}")
    Data getDataById(@PathVariable int dataId);

    @GetMapping(path = "/api/personalDataList")
    List<Data> getListPersonalData();

   /* @GetMapping(path = "/dataType/{id}")
    DataType getDataType(@PathVariable int id);*/

}
