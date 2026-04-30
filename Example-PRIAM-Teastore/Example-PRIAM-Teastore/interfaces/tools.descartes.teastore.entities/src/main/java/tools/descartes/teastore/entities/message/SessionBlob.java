package tools.descartes.teastore.entities.message;

import java.util.LinkedList;
import java.util.List;
import tools.descartes.teastore.entities.Order;
import tools.descartes.teastore.entities.OrderItem;

public class SessionBlob {
    private Long uid;
    private String sid;
    private String token;
    private String authToken; // Votre ajout
    private Order order;
    private List<OrderItem> orderItems = new LinkedList<OrderItem>();
    private String message;
    
    public SessionBlob() {
        this.setOrder(new Order());
    }

    
    public Long getUID() { return uid; }
    public void setUID(Long uID) { uid = uID; }
    
    public String getSID() { return sid; }
    public void setSID(String sID) { sid = sID; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    
    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }
}
