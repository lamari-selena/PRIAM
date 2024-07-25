package org.provider_microservice.services;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ProviderViewService {
    /**
     * Apply a rectification request
     * @param dataName Name of the Data
     * @param newValue New value
     * @param userId Reference ID of the user
     * @param dataTypeName Name of the DataType
     * @param primaryKeys PrimaryKeys of the Data
     * @throws SQLException
     */
    public void Rectification(String dataName, String newValue, String userId, String dataTypeName, Map<String, String> primaryKeys) throws SQLException;

    /**
     * Apply a erasure request
     * @param dataName Name of the Data
     * @param userId Reference ID of the user
     * @param dataTypeName Name of the DataType
     * @param primaryKeys PrimaryKeys of the Data
     */
    public void Erasure(String dataName, String userId, String dataTypeName, Map<String, String> primaryKeys) throws SQLException;

    /**
     * Retrieve the value of all Data of one user
     * @param idRef Reference ID of the user
     * @param dataTypeName Name of the DataType
     * @param attributes Names of the Data
     * @param primaryKeys PrimaryKeys List of the Datas
     * @return
     * @throws SQLException
     */
    List<Map<String,String>> getPersonalDataValues(String idRef, String dataTypeName, List<String> attributes, HashMap<String, String> primaryKeys) throws SQLException;

    /**
     * Retrieve the value of one Data for one user
     * @param idRef Reference ID of the user
     * @param dataName Name of the Data
     * @param primaryKeys PrimaryKeys of the Data
     * @return
     */
    Map<String, String> getDataValue(String idRef, String dataName, Map<String, String> primaryKeys);


}
