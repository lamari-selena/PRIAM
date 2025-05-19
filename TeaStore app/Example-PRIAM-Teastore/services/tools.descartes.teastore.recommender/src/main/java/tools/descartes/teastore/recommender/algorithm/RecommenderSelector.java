/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.descartes.teastore.recommender.algorithm;

import java.io.Console;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import ch.qos.logback.core.CoreConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.descartes.teastore.recommender.algorithm.impl.UseFallBackException;
import tools.descartes.teastore.recommender.algorithm.impl.cf.PreprocessedSlopeOneRecommender;
import tools.descartes.teastore.recommender.algorithm.impl.cf.SlopeOneRecommender;
import tools.descartes.teastore.recommender.algorithm.impl.orderbased.OrderBasedRecommender;
import tools.descartes.teastore.recommender.algorithm.impl.pop.PopularityBasedRecommender;
import tools.descartes.teastore.entities.Order;
import tools.descartes.teastore.entities.OrderItem;

/**
 * A strategy selector for the Recommender functionality.
 *
 * @author Johannes Grohmann
 *
 */
public final class RecommenderSelector implements IRecommender {

    /**
     * This map lists all currently available recommending approaches and
     * assigns them their "name" for the environment variable.
     */
    private static Map<String, Class<? extends IRecommender>> recommenders = new HashMap<>();

    static {
        recommenders = new HashMap<String, Class<? extends IRecommender>>();
        recommenders.put("Popularity", PopularityBasedRecommender.class);
        recommenders.put("SlopeOne", SlopeOneRecommender.class);
        recommenders.put("PreprocessedSlopeOne", PreprocessedSlopeOneRecommender.class);
        recommenders.put("OrderBased", OrderBasedRecommender.class);
    }

    /**
     * The default recommender to choose, if no other recommender was set.
     */
    private static final Class<? extends IRecommender> DEFAULT_RECOMMENDER = SlopeOneRecommender.class;

    private static final Logger LOG = LoggerFactory.getLogger(RecommenderSelector.class);

    private static RecommenderSelector instance;

    private IRecommender fallbackrecommender;

    private IRecommender recommender;

    private HttpClient client = HttpClient.newHttpClient();
    //http://localhost:8090/cdp/api/decision/1?idRefList=
    //http://localhost:8089/api/decision/1?idRefList=
    //http://localhost:8090/cdp/api/contract/1?idRefList=
    private HttpRequest builderHttp(Long userid, String processingId) {
        // Utilisez le nom de la méthode et de la classe dans l'URL
        String url = "http://172.17.0.1:8090/cdp/api/decision/" + processingId + "?idRefList=" + userid.toString();
        return HttpRequest.newBuilder().uri(URI.create(url)).build();
    }

    // Cette méthode envoie la requête HTTP et affiche la réponse
    private HttpResponse<String> sendReq(Long userid, String processingId) throws InterruptedException, IOException {
        System.out.println("yes" + processingId);
        HttpResponse<String> response = client.send(builderHttp(userid, processingId), HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP response status: " + response.statusCode());
        System.out.println("HTTP response body: " + response.body());
        return response;
    }

    // Cette méthode récupère le consentement et utilise le nom de la méthode et de la classe
    private boolean getConsent(Long userid, String processingId) throws InterruptedException, IOException {
        long startTime = System.currentTimeMillis();
        System.out.println("startTime getConsent" + startTime);

        HttpResponse<String> response = sendReq(userid, processingId);

        // Vérifiez si la réponse est correcte et non vide
        if (response.body() == null || response.body().isEmpty()) {
            System.out.println("La réponse est vide ou nulle !");
            return false;
        }

        try {
            JSONObject myObject = new JSONObject(response.body());
            boolean result = myObject.getBoolean(userid.toString());

            System.out.println("time of getConsent ---->" + (System.currentTimeMillis()- startTime));
           return result;
           // return myObject.getBoolean(userid.toString());
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de la réponse JSON : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Private Constructor.
     */
    private RecommenderSelector() {
        fallbackrecommender = new PopularityBasedRecommender();
        try {
            String recommendername = (String) new InitialContext().lookup("java:comp/env/recommenderAlgorithm");
            // if a specific algorithm is set, we can use that algorithm
            if (recommenders.containsKey(recommendername)) {
                try {
                    recommender = recommenders.get(recommendername).getDeclaredConstructor().newInstance();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else {
                LOG.warn("Recommendername: " + recommendername
                        + " was not found. Using default recommender (SlopeOneRecommeder).");
                try {
                    recommender = DEFAULT_RECOMMENDER.getDeclaredConstructor().newInstance();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        } catch (InstantiationException | IllegalAccessException e) {
            // if creating a new instance fails
            e.printStackTrace();
            LOG.warn("Could not create an instance of the requested recommender. Using fallback.");
            recommender = fallbackrecommender;
        } catch (NamingException e) {
            // if nothing was set
            LOG.info("Recommender not set. Using default recommender (SlopeOneRecommeder).");
            try {
                try {
                    recommender = DEFAULT_RECOMMENDER.getDeclaredConstructor().newInstance();
                } catch (IllegalArgumentException e1) {
                    e1.printStackTrace();
                } catch (InvocationTargetException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    e1.printStackTrace();
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                }
            } catch (InstantiationException | IllegalAccessException e1) {
                // also the default algorithm could fail
                e1.printStackTrace();
                LOG.warn("Could not create an instance of DEFAULT_RECOMMENDER " + DEFAULT_RECOMMENDER.getName() + ".");
                recommender = fallbackrecommender;
            }
        }
    }
  /* @Override
    public List<Long> recommendProducts(Long userid, List<OrderItem> currentItems)
            throws UnsupportedOperationException {

       System.out.println("exemple de userid et currentItems " + userid + "ms" + currentItems);
        long startTime = System.currentTimeMillis(); // Mesure du temps de début
        System.out.println("Le temps de début est de " + startTime + "ms");

        try {
            List<Long> result = recommender.recommendProducts(userid, currentItems); // Appel du premier recommander

            long endTime = System.currentTimeMillis(); // Mesure du temps de fin après l'exécution
            long diff = endTime - startTime; // Calcul de la différence


            return result; // Retourne le résultat
        } catch (UseFallBackException e) {
            // Si une exception est levée, on passe au fallback recommender
            LOG.trace("Executing " + recommender.getClass().getName()
                    + " as recommender failed. Using fallback recommender. Reason:\n" + e.getMessage());

            List<Long> result = fallbackrecommender.recommendProducts(userid, currentItems); // Appel du fallback recommender
            long endTime = System.currentTimeMillis(); // Mesure du temps de fin après l'exécution du fallback
            long diff = endTime - startTime; // Calcul de la différence
            System.out.println("Le temps de réponse (fallback) est de " + diff + "ms");

            return result; // Retourne le résultat du fallback
        } catch (UnsupportedOperationException e) {
            // Si l'algorithme n'est pas encore formé
            LOG.error("Executing " + recommender.getClass().getName()
                    + " threw an UnsupportedOperationException. The recommender was not finished with training.");
            throw e; // Relance l'exception
        } catch (Exception e) {
            // Si une autre exception inattendue se produit
            LOG.warn("Executing " + recommender.getClass().getName()
                    + " threw an unexpected error. Using fallback recommender. Reason:\n" + e.getMessage());

            List<Long> result = fallbackrecommender.recommendProducts(userid, currentItems); // Appel du fallback

            long endTime = System.currentTimeMillis(); // Mesure du temps de fin après l'exécution du fallback
            long diff = endTime - startTime; // Calcul de la différence
            System.out.println("Le temps de réponse (exception) est de " + diff + "ms");

            return result; // Retourne le résultat du fallback
        }
    }*/

    @Override
    public List<Long> recommendProducts(Long userid, List<OrderItem> currentItems) throws UnsupportedOperationException {
        boolean canUse;
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        String className = new Object() {}.getClass().getEnclosingClass().getSimpleName();
        String processingId = className+"."+methodName;
        System.out.println("USER ID ========> " + userid);
        try {
            canUse = getConsent(userid, processingId);
        } catch (InterruptedException | IOException ex) {
            LOG.trace("Executing " + recommender.getClass().getName()
                    + " with getConsent result in an error. Reason:\n" + ex.getMessage());
            return new ArrayList<>();
        }

        List<Long> result = new ArrayList<>();

        try {
            if (canUse) {
                result = recommender.recommendProducts(userid, currentItems);
            } else {
                result = new ArrayList<>();
            }
        } catch (UseFallBackException e) {
            // a UseFallBackException is usually ignored
            LOG.trace("Executing " + recommender.getClass().getName()
                    + " as recommender failed. Using fallback recommender. Reason:\n" + e.getMessage());
            if (canUse) {
                result = fallbackrecommender.recommendProducts(userid, currentItems);
            } else {
                result = new ArrayList<>();
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
                result = fallbackrecommender.recommendProducts(userid, currentItems);
            } else {
                result = new ArrayList<>();
            }
        }

        return result;
    }
    /**
     * Returns the instance of this Singleton or creates a new one, if this is
     * the first call of this method.
     *
     * @return The instance of this class.
     */
    public static synchronized RecommenderSelector getInstance() {
        if (instance == null) {
            instance = new RecommenderSelector();
        }
        return instance;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * tools.descartes.teastore.recommender.IRecommender#train(java.util.List,
     * java.util.List)
     */
    @Override
    public void train(List<OrderItem> orderItems, List<Order> orders) {
        recommender.train(orderItems, orders);
        fallbackrecommender.train(orderItems, orders);
    }

}