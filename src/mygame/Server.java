package mygame;

import mygame.multiplayer.server.MainServer;

/**
 * The program to start/stop the server.
 *
 * @author Felix Nguyen
 * @version 1
 */
public class Server
{
    /**
     * Entry point to the program.
     *
     * @param args none to start the server, or "interrupt" to stop it
     */
    public static void main(final String[] args)
    {
        if(args.length == 0)
        {
            new MainServer();
        } else if(args[0].equalsIgnoreCase("interrupt"))
        {
            MainServer.interrupt();
        } else
        {
            System.out.println("Invalid command: " + args[0]);
            System.out.println("Usage: server [interrupt]");
            System.exit(1);
        }
    }

    private Server() {}
}
