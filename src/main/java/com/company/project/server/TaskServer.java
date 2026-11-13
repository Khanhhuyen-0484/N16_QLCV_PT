package com.company.project.server;

import com.company.project.service.AccountService;
import com.company.project.service.AuthService;
import com.company.project.service.DataInitializer;
import com.company.project.service.ProjectService;
import com.company.project.service.SessionManager;
import com.company.project.service.TaskService;
import com.company.project.util.EntityManagerFactoryUtil;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server TCP: lắng nghe trên một cổng, mỗi client kết nối được xử lý bởi một luồng trong thread pool.
 */
public class TaskServer {
    public static final int DEFAULT_PORT = 2026;

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        new DataInitializer().initialize();

        SessionManager sessions = new SessionManager();
        RequestDispatcher dispatcher = new RequestDispatcher(
                new AuthService(sessions),
                new AccountService(sessions),
                new ProjectService(sessions),
                new TaskService(sessions));
        ExecutorService pool = Executors.newCachedThreadPool();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            pool.shutdownNow();
            EntityManagerFactoryUtil.close();
        }));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TaskServer đang chạy tại port " + port);
            System.out.println("Tài khoản Admin mặc định: admin / admin123");
            while (true) {
                Socket socket = serverSocket.accept();
                pool.submit(new ClientWorker(socket, dispatcher));
            }
        }
    }
}
