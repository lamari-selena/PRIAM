# Example Integration with Teastore

## Configure PRIAM and Keycloak

Initially, we will follow what is outlined on [the PRIAM integration page](./INTEGRATION.md).

This means we will use the default PRIAM configuration and the Keycloak configuration specified on [the installation page](./INSTALL.md).

## Configure the Teastore Application

Our goal is to modify the `Recommender` provided by Teastore. To achieve this, we will modify the file located at `services/tools.descartes.teastore.recommender/src/main/java/tools/descartes/teastore/recommender/algorithm/RecommenderSelector.java`.

To simplify things, we will directly write these functions into the `RecommenderSelector` class:

```java
    private HttpClient client = HttpClient.newHttpClient();

    private HttpRequest builderHttp(Long userid) {
        return HttpRequest.newBuilder().uri(URI.create("http://localhost:8089/api/decision/1?idRefList=" + userid.toString())).build();
    }

    private HttpResponse sendReq(Long userid) throws InterruptedException, IOException {
        return client.send(builderHttp(userid), HttpResponse.BodyHandlers.ofString());
    }

    private boolean getConsent(Long userid) throws InterruptedException, IOException {
        JSONObject myObject = new JSONObject(sendReq(userid).body());
        return myObject.getBoolean(userid.toString());
    }
```

We will use `getConsent` with `userid` to directly communicate with the PRIAM service.

Now, we will modify the `recommendProducts` function so that if the user does not give their consent, the function will not be executed:

```java
@Override
    public List<Long> recommendProducts(Long userid, List<OrderItem> currentItems)
            throws UnsupportedOperationException {
        boolean canUse;
        try {
            canUse = getConsent(userid);
        } catch (InterruptedException | IOException ex) {
            LOG.trace("Executing " + recommender.getClass().getName()
                    + " with getConsent result in an error. Reason:\n" + ex.getMessage());
            return new ArrayList<>();
        }
        try {
            if (canUse) {
                return recommender.recommendProducts(userid, currentItems);
            } else {
                return new ArrayList<>();
            }
        } catch (UseFallBackException e) {
            // a UseFallBackException is usually ignored (as it is conceptual and might
            // occur quite often)
            LOG.trace("Executing " + recommender.getClass().getName()
                    + " as recommender failed. Using fallback recommender. Reason:\n" + e.getMessage());
            if (canUse) {
                return fallbackrecommender.recommendProducts(userid, currentItems);
            } else {
                return new ArrayList<>();
            }
        } catch (UnsupportedOperationException e) {
            // if algorithm is not yet trained, we throw the error
            LOG.error("Executing " + recommender.getClass().getName()
                    + " threw an UnsupportedOperationException. The recommender was not finished with training.");
            throw e;
        } catch (Exception e) {
            // any other exception is just reported
            LOG.warn("Executing " + recommender.getClass().getName()
                    + " threw an unexpected error. Using fallback recommender. Reason:\n" + e.getMessage());
            if (canUse) {
                return fallbackrecommender.recommendProducts(userid, currentItems);
            } else {
                return new ArrayList<>();
            }

        }
    }
```

You now have an application that depends on user consent!