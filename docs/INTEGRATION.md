# How to Integrate PRIAM into Your Application

To integrate PRIAM into your application, several steps are necessary to ensure optimal configuration and smooth communication between the various components. Follow these steps:

- Properly configure PRIAM for your application, including choosing appropriate ports, passwords, etc.
- Use distinct function names.
- Be able to make HTTP requests.

## First Step: PRIAM

PRIAM needs to be configured to work within your system. For this, refer to the documentation on [installation](./INSTALL.md).

Make sure to memorize important endpoints, such as those for Keycloak and the CEP.

## Second Step: Keycloak and Your Services

### Keycloak Configuration

Keycloak needs to be configured to recognize your services. This includes registering your application and defining the necessary roles and permissions. Follow these steps:

1. **Access the Keycloak Admin Console**: Log in to the Keycloak admin console.
2. **Create a New Client**:
   - Go to the "Clients" section and click "Create".
   - Enter the necessary information, such as the client name and redirection URL.
3. **Configure Users**:
   - Create users and assign them the appropriate roles.

You will find everything on [the installation page](./INSTALL.md).

## Third Step: Configuring Your Application

### Integration with Keycloak

1. **Add Keycloak Dependencies**: Add the Keycloak dependencies to your project. For example, for a Java project using Maven, add the following dependency to your `pom.xml`:

   ```xml
   <dependency>
       <groupId>org.keycloak</groupId>
       <artifactId>keycloak-spring-boot-starter</artifactId>
       <version>${keycloak.version}</version>
   </dependency>
   ```

2. **Configure Keycloak in Your Application**: Add the Keycloak configuration to your configuration file (for example, `application.yml` for a Spring Boot application):

   ```yaml
   keycloak:
     realm: priam-realm
     auth-server-url: http://localhost:8080/auth
     resource: your-client-id
     credentials:
       secret: your-client-secret
     use-resource-role-mappings: true
   ```

You then need to link your users and `idReference` so that PRIAM recognizes your users.

## Fourth Step: Making Requests to PRIAM

To enable your application to interact with PRIAM, you need to be able to send HTTP requests to the appropriate endpoints. You can use different methods depending on the language and framework you are using. Here are a few examples:

#### Example in Java with `HttpClient`:

##### With only one user per request

```java
    private HttpClient client = HttpClient.newHttpClient();

    // Build the URL request.
    private HttpRequest builderHttp(Long userID, int processID) {
        return HttpRequest.newBuilder().uri(URI.create("http://localhost:8089/api/decision/" + processID + "?idRefList=" + userID.toString())).build();
    }

    // Send the request to the CEP
    private HttpResponse sendReq(Long userID, int processID) throws InterruptedException, IOException {
        return client.send(builderHttp(userID, processID), HttpResponse.BodyHandlers.ofString());
    }


    // Return a boolean base of the response of the CEP
    private boolean getConsent(Long userID, int processID) throws InterruptedException, IOException {
        JSONObject myObject = new JSONObject(sendReq(userID, processID).body());
        return myObject.getBoolean(userID.toString());
    }


```

##### With multiple users per request

```java
    private HttpClient client = HttpClient.newHttpClient();
    // Build the URL request.
    private HttpRequest builderHttpList(ArrayList<Long> userID, int processID) {
        return HttpRequest.newBuilder().uri(URI.create("http://localhost:8089/api/decision/" + processID + "?" + userID.stream().map(id -> "idRefList=" + id).collect(StringBuilder::new, (x, y) -> x.append(y),
                (a, b) -> a.append(",").append(b)).toString())).build();
    }

        // Send the request to the CEP
    private HttpResponse sendReqList(ArrayList<Long> userID, int processID) throws InterruptedException, IOException {
        return client.send(builderHttpList(userID, processID), HttpResponse.BodyHandlers.ofString());
    }

    // Return a boolean base of the response of the CEP
    private ArrayList<Long> getConsentList(ArrayList<Long> userID, int processID) throws InterruptedException, IOException {
        JSONObject myObject = new JSONObject(sendReqList(userID, processID).body());
        return (ArrayList<Long>) userID.stream().filter(obj -> myObject.getBoolean(obj.toString())).collect(Collectors.toList());
    }

```
