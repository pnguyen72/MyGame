package MyGame;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import MyGame.Client.CPU;
import MyGame.Client.Client;
import MyGame.Game.TicTacToe;
import MyGame.Server.MainServer;
import MyGame.Services.Scheduler;

/**
 * Tests for server-client handling
 */
class ServerTest
{
    private static MainServer server;

    /**
     * Test that the server can be started.
     * Run before all so that the server is live during the rest of the tests.
     */
    @BeforeAll
    static void startServer()
    {
        MainServer.interrupt(); // in case another server is already running
        Scheduler.wait(500);

        server = new MainServer();
        assertTrue(MainServer.isRunning());
    }

    /**
     * Test that the server can be interrupted.
     * Run after all so that the server is live during the rest of the tests.
     */
    @AfterAll
    static void stopServer()
    {
        assertTrue(MainServer.isRunning());
        MainServer.interrupt();
        Scheduler.wait(500);
        assertFalse(MainServer.isRunning());
    }

    /**
     * Starting the server should fail if the server is already running.
     */
    @Test
    void serverAlreadyRunning()
    {
        final Exception e;
        e = assertThrows(IllegalStateException.class, MainServer::new);
        assertEquals("Server is already running", e.getMessage());
    }

    /**
     * Server should recognize
     * when a client joins the request queue and when they leave.
     */
    @Test
    void requestsHandling()
    {
        final Client client;
        final String clientID;
        final Path   request;

        client   = new CPU();
        clientID = client.getClientID();

        Scheduler.wait(500);
        request = server.getRequests().getFirst();
        assertNotNull(request);
        assertEquals(clientID, request.getFileName().toString());

        client.stop();

        Scheduler.wait(1000);
        assertNull(server.getRequests().getFirst());
        assertFalse(Files.exists(GameFiles.getRequest(clientID)));
    }

    /**
     * Server should match the top 2 clients in the request queue
     * and create a game for them.
     */
    @Test
    void gameMatching()
    {
        final Client client1;
        final Client client2;
        client1 = new CPU();
        client2 = new CPU();

        Scheduler.wait(1000);
        assertEquals(0, server.getRequests().size());
        Scheduler.wait(1000);
        assertEquals(client1.getGameID(), client2.getGameID());

        /*-----------------*/

        final Client client3;
        final Client client4;
        client3 = new CPU();
        client4 = new CPU();

        Scheduler.wait(1000);
        assertEquals(0, server.getRequests().size());
        Scheduler.wait(1000);
        assertEquals(client3.getGameID(), client4.getGameID());
    }

    /**
     * When a client is disconnected mid-game, the other one should win.
     */
    @Test
    void clientDisconnect()
    {
        final Client client1;
        final Client client2;
        client1 = new CPU();
        client2 = new CPU();

        Scheduler.wait(1000);
        client1.stop();

        Scheduler.wait(1500);
        assertEquals(TicTacToe.Status.WON, client2.getGameStatus());

        /*-----------------*/

        final Client client3;
        final Client client4;
        client3 = new CPU();
        client4 = new CPU();

        Scheduler.wait(1000);
        client4.stop();

        Scheduler.wait(1500);
        assertEquals(TicTacToe.Status.WON, client3.getGameStatus());
    }
}
