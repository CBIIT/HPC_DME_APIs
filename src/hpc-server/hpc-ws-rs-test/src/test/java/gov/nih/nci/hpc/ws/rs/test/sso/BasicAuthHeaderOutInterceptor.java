package gov.nih.nci.hpc.ws.rs.test.sso;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class BasicAuthHeaderOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(BasicAuthHeaderOutInterceptor.class);
    
    public BasicAuthHeaderOutInterceptor() {
        this(Phase.WRITE);
    }
    
    public BasicAuthHeaderOutInterceptor(String phase) {
        super(phase);
    }
    
    public void handleMessage(Message message) throws Fault {
        try {
            String name = "inttest";
            String pass = "pwd";
  
            String authString = name + ":" + pass;
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);
              
            Map<String, List<String>> headers = getHeaders(message);
            
            StringBuilder builder = new StringBuilder();
            builder.append("Basic").append(" ").append(authStringEnc);
            headers.put("Authorization", 
                CastUtils.cast(Collections.singletonList(builder.toString()), String.class));
            
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            LOG.warning(sw.toString());
            throw new Fault(new RuntimeException(ex.getMessage() + ", stacktrace: " + sw.toString()));
        }
        
    }
        
    private Map<String, List<String>> getHeaders(Message message) {
        Map<String, List<String>> headers = 
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        if (headers == null) {
            headers = new HashMap<>();
            message.put(Message.PROTOCOL_HEADERS, headers);
        }
        return headers;
    }
    
    
}

