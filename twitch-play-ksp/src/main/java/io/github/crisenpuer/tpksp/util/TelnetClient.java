package io.github.crisenpuer.tpksp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

public class TelnetClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread keepAliveThread;
    private KeepAliveSender keepAliveSender;
    private static Logger logger = Logger.getLogger("TelnetClient");

    /**
     * Connects to a Telnet server.
     *
     * @param host the IP address or hostname of the Telnet server
     * @param port the port number (default for Telnet is 23)
     * @param keepAlive whether or not to have socket keep alive turned on
     * @throws IOException if an I/O error occurs when creating the socket
     */
    public void connect(String host, int port, boolean keepAlive) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        logger.info("Connected to the server.");
        socket.setKeepAlive(keepAlive);
        if (keepAlive) {
            this.keepAliveSender = new KeepAliveSender(socket);
            this.keepAliveThread = new Thread(this.keepAliveSender);
            this.keepAliveThread.start();
        }
    }

    /**
     * Sends a command to the Telnet server.
     *
     * @param command the command to send
     */
    public void sendCommand(String command) {
        writer.println(command);
        writer.flush();
    }
    public void sendCommand(int asciiCode) {
        writer.write(asciiCode);
        writer.flush();
    }

    /**
     * Reads a single line of output from the Telnet server.
     *
     * @return the line read, or null if the end of the stream is reached
     * @throws IOException if an I/O error occurs
     */
    public String readOutput() throws IOException {
        return reader.readLine();
    }

    /**
     * Closes the connection to the Telnet server.
     *
     * @throws IOException if an I/O error occurs when closing the socket
     */
    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            if (keepAliveSender != null) {
                keepAliveSender.stop();
                try {
                    keepAliveThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warning("Thread was interrupted while waiting for keep-alive sender to stop.");
                }
            }
        }
    }
    /**
     * Checks if the Telnet client is currently connected to the server.
     *
     * @return true if connected; false otherwise.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}