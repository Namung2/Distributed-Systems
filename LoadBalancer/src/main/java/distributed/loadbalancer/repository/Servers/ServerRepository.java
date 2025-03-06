package distributed.loadbalancer.repository.Servers;

import java.util.HashSet;
import java.util.Set;

public class ServerRepository {

    private static final Set<Server> registeredServers = new HashSet<>();
    private static  final ServerRepository instance = new ServerRepository();
    private ServerRepository() {}

    public static ServerRepository getInstance() {return instance;}

    public  Boolean addServer(Server server)
        {return registeredServers.add(server);}

    public  Boolean removeServer(Server server)
        {return registeredServers.remove(server);}

    public  Set<Server> checkAllServers()
        {return new HashSet<>(registeredServers);}
}
