package gov.nih.nci.hpc.ws.rs.test.sso;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;

/**
 */

public class KeystorePasswordCallback implements CallbackHandler {
    
    public KeystorePasswordCallback() {
    }

    /**
     * It attempts to get the password from the private 
     * alias/passwords map.
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
            if ("alice".equals(pc.getIdentifier())) {
                pc.setPassword("password");
            }
        }
    }
    

}
