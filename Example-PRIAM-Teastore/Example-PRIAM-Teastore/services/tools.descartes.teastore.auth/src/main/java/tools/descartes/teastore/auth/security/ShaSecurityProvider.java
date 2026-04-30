package tools.descartes.teastore.auth.security;

import tools.descartes.teastore.entities.message.SessionBlob;

/**
 * Security provider that preserves authToken during security operations.
 * 
 * @author Simon
 */
public class ShaSecurityProvider implements ISecurityProvider {

  @Override
  public IKeyProvider getKeyProvider() {
    return new ConstantKeyProvider();
  }

  @Override
  public SessionBlob secure(SessionBlob blob) {
    if (blob == null || blob.getUID() == null || blob.getSID() == null) {
      return blob;
    }
    
    // Preserve the authToken throughout the security process
    String originalAuthToken = blob.getAuthToken();
    
    // Generate security token based on UID and SID
    String token = generateToken(blob);
    blob.setToken(token);
    
    // Ensure authToken is preserved
    blob.setAuthToken(originalAuthToken);
    
    return blob;
  }

  @Override
  public SessionBlob validate(SessionBlob blob) {
    if (blob == null || blob.getUID() == null || blob.getSID() == null) {
      return null;
    }
    
    // Validate the security token
    String currentToken = blob.getToken();
    String expectedToken = generateToken(blob);
    
    if (currentToken != null && currentToken.equals(expectedToken)) {
      return blob;
    }
    
    return null;
  }

  private String generateToken(SessionBlob blob) {
    if (blob.getUID() == null || blob.getSID() == null) {
      return null;
    }
    // Simple token generation - you might want to make this more secure
    return String.valueOf(Math.abs(blob.getUID().hashCode() + blob.getSID().hashCode()));
  }
}
