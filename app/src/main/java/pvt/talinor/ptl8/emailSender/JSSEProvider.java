package pvt.talinor.ptl8.emailSender;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;

/**
 * <p> Created by Rubezhin Evgenij on 7/1/2019. <br>
 * Copyright (c) 2019 LineUp. <br> Project: bm71term, pvt.talinor.ptl8.emailSender </p>
 *
 * @author Rubezhin Evgenij
 * @version 1.0
 */
public class JSSEProvider extends Provider {

  /**
   * Constructs a provider with the specified name, version number, and information.
   *
   * @param name the provider name.
   * @param version the provider version number.
   * @param info a description of the provider and its services.
   */
  protected JSSEProvider(String name, double version, String info) {
    super(name, version, info);
  }

  public JSSEProvider() {
    super("HarmonyJSSE", 1.0, "Harmony JSSE Provider");
    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
      put("SSLContext.TLS",
          "org.apache.harmony.xnet.provider.jsse.SSLContextImpl");
      put("Alg.Alias.SSLContext.TLSv1", "TLS");
      put("KeyManagerFactory.X509",
          "org.apache.harmony.xnet.provider.jsse.KeyManagerFactoryImpl");
      put("TrustManagerFactory.X509",
          "org.apache.harmony.xnet.provider.jsse.TrustManagerFactoryImpl");
      return null;
    });
  }
}
