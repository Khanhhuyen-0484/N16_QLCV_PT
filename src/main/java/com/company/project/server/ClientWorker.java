package com.company.project.server;

import com.company.project.network.Request;
import com.company.project.network.Response;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/** Xử lý một client: đọc Request (ObjectInputStream), gửi Response (ObjectOutputStream) cho tới khi client ngắt. */
public class ClientWorker implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher;

    public ClientWorker(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        String client = socket.getRemoteSocketAddress().toString();
        System.out.println("[+] Client kết nối: " + client + " (" + Thread.currentThread().getName() + ")");
        try (socket;
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Object object = input.readObject();
                Response response;
                if (object instanceof Request request) {
                    response = dispatcher.dispatch(request);
                    System.out.println("    " + client + " -> " + request.getAction()
                            + (response.isSuccess() ? " [OK]" : " [LỖI] " + response.getMessage()));
                } else {
                    response = Response.error("Gói tin không hợp lệ.");
                }
                output.writeObject(response);
                output.flush();
                output.reset();
            }
        } catch (EOFException | SocketException ignored) {
            // Client đóng kết nối
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            System.out.println("[-] Client ngắt kết nối: " + client);
        }
    }
}
