package com.company.project.server;

import com.company.project.network.Request;
import com.company.project.network.Response;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientWorker implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher;

    public ClientWorker(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try (socket;
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Object object = input.readObject();
                if (!(object instanceof Request request)) {
                    output.writeObject(Response.error("Gói tin không hợp lệ."));
                    output.flush();
                    continue;
                }
                Response response = dispatcher.dispatch(request);
                output.writeObject(response);
                output.flush();
            }
        } catch (EOFException ignored) {
            // Client closed the connection.
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
