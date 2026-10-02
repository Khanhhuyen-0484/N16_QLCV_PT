package com.company.project.server;

import com.company.project.service.AppService;
import com.company.project.util.JpaUtil;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskServer {
    public static final int DEFAULT_PORT = 2026;

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        AppService service = new AppService();
        service.seedDefaultManager();
        service.seedDemoData();
        RequestDispatcher dispatcher = new RequestDispatcher(service);
        ExecutorService pool = Executors.newCachedThreadPool();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            pool.shutdownNow();
            JpaUtil.close();
        }));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TaskServer đang chạy tại port " + port);
            System.out.println("Tài khoản quản lý mặc định: admin / admin123");
            while (true) {
                Socket socket = serverSocket.accept();
                pool.submit(new ClientWorker(socket, dispatcher));
            }
        }
    }
}
