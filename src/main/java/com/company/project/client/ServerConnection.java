package com.company.project.client;

import com.company.project.network.Request;
import com.company.project.network.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Kết nối TCP tới TaskServer, dùng chung cho client console và client Swing.
 * send() được đồng bộ hóa (synchronized) để nhiều luồng SwingWorker không ghi/đọc xen kẽ trên cùng một socket.
 */
public class ServerConnection implements AutoCloseable {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public ServerConnection(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        open();
    }

    private void open() throws IOException {
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
    }

    public synchronized Response send(Request request) {
        try {
            return exchange(request);
        } catch (IOException | ClassNotFoundException firstError) {
            // Server có thể vừa khởi động lại: thử kết nối lại một lần
            try {
                closeQuietly();
                open();
                return exchange(request);
            } catch (IOException | ClassNotFoundException e) {
                return Response.error("Không kết nối được server " + host + ":" + port + " (" + e.getMessage() + ").");
            }
        }
    }

    private Response exchange(Request request) throws IOException, ClassNotFoundException {
        output.writeObject(request);
        output.flush();
        output.reset();
        return (Response) input.readObject();
    }

    public String address() {
        return host + ":" + port;
    }

    private void closeQuietly() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // bỏ qua
        }
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }
}
