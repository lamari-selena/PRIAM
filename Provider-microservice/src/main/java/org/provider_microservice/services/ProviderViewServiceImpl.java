package org.provider_microservice.services;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Generated;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.provider_microservice.mappers.ProviderViewMapper;
import org.provider_microservice.repositories.ProviderViewRepository;
import org.springframework.stereotype.Service;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        date = "2021-05-23T23:03:41+0530"
)
@Service
@Transactional
public class ProviderViewServiceImpl implements ProviderViewService {
    final ProviderViewRepository providerViewRepository;
    final ProviderViewMapper providerViewMapper;
    @PersistenceContext
    private EntityManager entityManager;

    public ProviderViewServiceImpl(ProviderViewRepository providerViewRepository, ProviderViewMapper providerViewMapper) {
        this.providerViewRepository = providerViewRepository;
        this.providerViewMapper = providerViewMapper;
    }


    @Override
    public List<Map<String, String>> getPersonalDataValues(String idRef, String dataTypeName, List<String> attributes, HashMap<String, String> primaryKeys) throws SQLException {
        String listAttributes = "";
        for (String s : attributes) {
            if (listAttributes.equals("")) listAttributes = listAttributes + s;
            else listAttributes = listAttributes + "," + s;
        }
        String value = null;
        List<Map<String, String>> values = new ArrayList<>();

        String query = "SELECT DISTINCT " + listAttributes + " FROM provider_view WHERE pu_ID = :idRef";
        Query sqlQuery = entityManager.createNativeQuery(query);
        sqlQuery.setParameter("idRef", idRef);

        // Because when size == 1, the result is not a object list but a String list
        if (attributes.size() == 1) {
            List<String> results = sqlQuery.getResultList();
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i) == null) value = "no data available";
                else value = results.get(i);
                addData(values, createDataMap(attributes.get(0), value));
            }
        } else {
            List<Object[]> results = sqlQuery.getResultList();
            for (Object[] result : results) {
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == null) value = "no data available";
                    else value = result[i].toString();
                    addData(values, createDataMap(attributes.get(i), value));
                }
            }
        }
        return values;
    }
   /* @Override
    public List<Map<String,String>> getValeursPersonalData(int idDs, String dataTypeName, List<String> attributes) throws SQLException{
        System.out.println(attributes);
        List<Map< String,String>> values = new ArrayList<>();
        String listAttributes ="";

        for (String s: attributes) {
            if (listAttributes.equals("")) listAttributes = listAttributes + s;
            else listAttributes = listAttributes + "," + s;
        }
        System.out.println(listAttributes);
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3307/teadb", "root", "");
        PreparedStatement ps = con.prepareStatement("select distinct "+  listAttributes +  " from provider_view where pu_ID = " + idDs);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                for(String attribute: attributes) {
                    String value = rs.getString(attribute);
                    addData(values, createDataMap(attribute,value));
                    //values.add(column,value);
                }
                System.out.print(values);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des résultats de la requête : " + e.getMessage());
            e.printStackTrace();
        }

        return values;
}*/

    @Override
    public Map<String, String> getDataValue(String idRef, String dataName, Map<String, String> primaryKeys) {
        HashMap<String, String> response = new HashMap<>();
        String query = "SELECT DISTINCT " + dataName + " FROM provider_view WHERE pu_ID = :idRef";
        Query sqlQuery = entityManager.createNativeQuery(query);
        sqlQuery.setParameter("idRef", idRef);

        List<String> result = sqlQuery.getResultList();
        response.put("value", result.get(0));
        return response;
    }

    private static Map<String, String> createDataMap(String column, String value) {
        Map<String, String> data = new HashMap<>();
        data.put("attribute", column);
        data.put("value", value);
        System.out.println(data);
        return data;
    }

    private static void addData(List<Map<String, String>> dataList, Map<String, String> data) {
        dataList.add(data);
    }

    @Override
    public void Rectification(String dataName, String newValue, String userId, String dataTypeName, Map<String, String> primaryKeys) throws SQLException {
        String primaryKeyString = "";
        for (Map.Entry<String, String> entry : primaryKeys.entrySet()) {
            primaryKeyString += " and " + entry.getKey() + " = " + entry.getValue();
        }
        String query = "UPDATE provider_view set " + dataName + " = :newValue where pu_ID = :idDS" + primaryKeyString;

        Query sqlQuery = entityManager.createNativeQuery(query);
        //sqlQuery.setParameter("attribute", attribute);
        sqlQuery.setParameter("newValue", newValue);
        sqlQuery.setParameter("idDS", userId);
        //sqlQuery.setParameter("primaryKeyName", primaryKeyName);
        sqlQuery.executeUpdate();
        /* Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3307/teadb", "root", "");
        ;
        PreparedStatement ps = con.prepareStatement("UPDATE provider_view set " + attribute + " ='" + newValue + "'where pu_ID =" + patientId+ " and " + primaryKeyName + " = " + primaryKeyValue);
        int i = ps.executeUpdate();*/
    }

    @Override
    public void Erasure(String dataName, String userId, String dataTypeName, Map<String, String> primaryKeys) throws SQLException {
        String primaryKeyString = "";
        for (Map.Entry<String, String> entry : primaryKeys.entrySet()) {
            primaryKeyString += " and " + entry.getKey() + " = " + entry.getValue();
        }

        String query = "UPDATE provider_view set " + dataName + " = null where pu_ID = :idDS" + primaryKeyString;
        Query sqlQuery = entityManager.createNativeQuery(query);
        sqlQuery.setParameter("idDS", userId);
        sqlQuery.executeUpdate();
        /* Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3307/teadb", "root", "");
        ;
        PreparedStatement ps = con.prepareStatement("UPDATE provider_view set " + attribute + " ='" + newValue + "'where pu_ID =" + patientId+ " and " + primaryKeyName + " = " + primaryKeyValue);
        int i = ps.executeUpdate();*/
    }
}
