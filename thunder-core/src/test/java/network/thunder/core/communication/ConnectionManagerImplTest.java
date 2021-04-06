package network.thunder.core.communication;

import network.thunder.core.communication.layer.ContextFactoryImpl;
import network.thunder.core.database.InMemoryDBHandler;
import network.thunder.core.etc.ConnectionManagerWrapper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.SeedNodes;
import network.thunder.core.helper.callback.results.NullResultCommand;
import network.thunder.core.helper.events.LNEventHelperImpl;
import network.thunder.core.helper.wallet.MockWallet;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionManagerImplTest {

    final static int AMOUNT_OF_NODES = 100;
    final static int PORT_START = 20000;

    ConnectionManagerWrapper connectionSeed;

    List<ConnectionManagerWrapper> clients = new ArrayList<>();

    @Before
    public void prepare () {
        createSeedConnection();
        createNodes();
    }

    //Take out this test for now until we are done refactoring ConnectionManager
    //    @Test
    public void test () throws Exception {
        Logger.getLogger("io.netty").setLevel(Level.OFF);

        connectionSeed.connectionManager.startListening(new NullResultCommand());

        for (int i = 6; i < 10; ++i) {

            System.out.println("------------------------------------------------------------------------------");
            System.out.println(i + " started up...");

            clients.get(i).connectionManager.fetchNetworkIPs(new NullResultCommand());
            clients.get(i).connectionManager.startSyncing(new NullResultCommand());
            clients.get(i).connectionManager.startBuildingRandomChannel(new NullResultCommand());
            Thread.sleep(500);
        }

        Thread.sleep(1000);
    }

    private void createSeedConnection () {
        SeedNodes.setToTestValues();
        ClientObject node = SeedNodes.nodeList.get(0);
        ServerObject serverObject = new ServerObject();
        serverObject.portServer = node.port;
        serverObject.hostServer = node.host;
        serverObject.pubKeyServer = node.pubKeyClient;

        connectionSeed = getConnection(serverObject);
    }

    private void createNodes () {
        for (int i = 0; i < AMOUNT_OF_NODES; ++i) {
            int port = PORT_START + i;
            String host = "127.0.0.1";

            ServerObject node = new ServerObject();
            node.init();
            node.portServer = port;
            node.hostServer = host;

            ConnectionManagerWrapper wrapper = getConnection(node);

            clients.add(wrapper);
        }
    }

    private ConnectionManagerWrapper getConnection (ServerObject node) {
        ConnectionManagerWrapper connection = new ConnectionManagerWrapper();
        connection.dbHandler = new InMemoryDBHandler();
        connection.wallet = new MockWallet(Constants.getNetwork());
        connection.contextFactory = new ContextFactoryImpl(node, connection.dbHandler, connection.wallet, new LNEventHelperImpl());
        connection.connectionManager = new ConnectionManagerImpl(connection.contextFactory, connection.dbHandler);
        return connection;
    }

}