# PRIAM Overview

PRIAM is an innovative solution specifically designed to help organizations ensure their applications comply with the stringent requirements of the General Data Protection Regulation (GDPR). As privacy and data protection become increasingly critical, businesses need a reliable framework to address the complexities of GDPR compliance, and PRIAM provides just that.

Please note that the containerized version is not yet available.

## Prerequisites

Before running PRIAM, make sure you have the following tools installed:

- IntelliJ IDEA (with Gradle support)
- Keycloak (for authentication, if your application doesn't have an existing authentication server)
- Angular
- MySQL (which can be managed via XAMPP)

## Getting Started

To get the application up and running, follow these steps:

1. Set up MySQL or start it via XAMPP.
2. Configure Keycloak if required, or connect to your existing authentication server.
3. Open the project in IntelliJ and build it using Gradle.

## Application Compliance Process

The PRIAM application focuses on ensuring compliance with GDPR, with particular emphasis on consent management. The relevant part of the solution involves:

### Consent Management

The primary feature covered in this version of PRIAM is consent management, based on the ABAC (Attribute-Based Access Control) pattern and the OAuth2 protocol. This allows applications to manage and record user consent in a compliant manner, ensuring that sensitive personal data is handled according to the GDPR.

You can test PRIAM on the [TeaStore case study](https://github.com/DescartesResearch/TeaStore).

## Integration steps

### Overview

Our goal, initially, is to modify the optional processings requiring users' explicit consent for their execution. These processings are only executed after verification of consent. Let's take the example of a personalized movie recommendation feature.

The goal is to verify whether the user, for whom we want to offer suggestions, has given their explicit consent before generating and presenting these suggestions.

### Example of Integration with PRIAM

To enable your application to interact with PRIAM, you need to be able to send HTTP requests to the appropriate endpoints. You can use different methods depending on the language and framework you are using. Here is an example of how this can be done using Java with `HttpClient`:

```java
private HttpClient client = HttpClient.newHttpClient();

private HttpRequest builderHttp(Long userid, String processingId) {
    // Build the URL request.
    String url = "http://localhost:8090/cdp/api/decision/" + processingId + "?idRefList=" + userid.toString();
    return HttpRequest.newBuilder().uri(URI.create(url)).build();
}

// Send the request to the CEP
private HttpResponse<String> sendReq(Long userid, String processingId) throws InterruptedException, IOException {
    HttpResponse<String> response = client.send(builderHttp(userid, processingId), HttpResponse.BodyHandlers.ofString());
    return response;
}

// Return a boolean based on the response from the CEP
private boolean getConsent(Long userID, int processID) throws InterruptedException, IOException {
    JSONObject myObject = new JSONObject(sendReq(userID, processID).body());
    return myObject.getBoolean(userID.toString());
}

This code checks, via the consent manager, whether the user (identified by "userId") has given their consent or not.

- Now, the code must be modified so that its execution directly depends on the response from `getConsent()` (see example).
  
- Remember that when annotating the processes, we defined the `processingId` as the class name followed by the method name (`className.methodName`). That’s why we added two variables: `className` and `methodName`, which respectively retrieve the class name and the method name.


```java
@Override
public List<Long> suggestMovies(Long userId, List<OrderItem> currentItems)
        throws UnsupportedOperationException {
    String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
    String className = new Object() {}.getClass().getEnclosingClass().getSimpleName();
    String processingId = className + "." + methodName;
    boolean canUse;
    
    // Votre logique métier ici
    
    return new ArrayList<>(); // return example
}


### Modify your code so that:

- `canUse` stores the response returned by the `getConsent(userId, processingId)` function.
- The `suggestMovies` processing only executes for the user in question if the response is positive (`true`).

```java
private HttpClient client = HttpClient.newHttpClient();

// Build the URL request.
private HttpRequest builderHttpList(ArrayList<Long> userID, int processID) {
    return HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8090/api/decision/" + processID + "?"
        + userID.stream()
            .map(id -> "idRefList=" + id)
            .collect(StringBuilder::new, (x, y) -> x.append(y), (a, b) -> a.append(",").append(b))
            .toString()))
        .build();
}

// Send the request to the CEP
private HttpResponse<String> sendReqList(ArrayList<Long> userID, int processID) throws
    InterruptedException, IOException {
    return client.send(builderHttpList(userID, processID), HttpResponse.BodyHandlers.ofString());
}

// Return a boolean based on the response of the CEP
private ArrayList<Long> getConsentList(ArrayList<Long> userID, int processID) throws
    InterruptedException, IOException {
    JSONObject myObject = new JSONObject(sendReqList(userID, processID).body());
    return (ArrayList<Long>) userID.stream()
        .filter(obj -> myObject.getBoolean(obj.toString()))
        .collect(Collectors.toList());
}


### Note:

- If the function takes a set of users as a parameter instead of a single user, this version of the code should be used.

- You might encounter an error if PRIAM and your application are not on the same network. In that case, remember to modify the URL by replacing "localhost" with the network to which your application belongs.
