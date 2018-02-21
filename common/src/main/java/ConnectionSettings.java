import lombok.Getter;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * @author Aleksey Yakovlev on 17.02.2018
 * @project CloudStorage
 */


public class ConnectionSettings {
    public static final String SERVER_IP="localhost";

    public static final int SERVER_PORT=8189;
    public static final Proxy CONNECTION_STRING = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(SERVER_IP,SERVER_PORT));

}
