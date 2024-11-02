package io.github.crisenpuer.tpksp.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class KeepAliveSender implements Runnable {
    private final Socket socket;
    private volatile boolean running = true; // Control flag

    public KeepAliveSender(Socket socket) {
        this.socket = socket;
    }

    public void stop() {
        running = false; // Set running to false to stop the thread
    }

    @Override
    public void run() {
        try {
            while (running && !socket.isClosed()) {
                // Send a heartbeat message
                OutputStream out = socket.getOutputStream();
                out.write(" ".getBytes());
                out.write(8);
                out.flush();
                Thread.sleep(5000); // Send every 5 seconds
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}