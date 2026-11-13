package com.company.project.client;

import com.company.project.dto.AccountSummaryView;
import com.company.project.dto.DashboardStats;
import com.company.project.dto.EmployeeView;
import com.company.project.dto.LoginResult;
import com.company.project.dto.ProjectView;
import com.company.project.dto.StatusHistoryView;
import com.company.project.dto.TaskView;
import com.company.project.entity.AccountStatus;
import com.company.project.entity.LeaveReason;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.RemovalReason;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.entity.TaskStatus;
import com.company.project.network.Action;
import com.company.project.network.Request;
import com.company.project.network.Response;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Client giao diện Swing.
 * Mọi yêu cầu gửi server đều chạy trong SwingWorker (luồng nền) để giao diện không bị đơ;
 * kết quả được cập nhật lên giao diện trong done() trên luồng EDT.
 */
public class TaskSwingClient extends JFrame {
    private final ServerConnection connection;
    private String token;
    private EmployeeView currentUser;

    private CardLayout cardLayout;
    private JPanel mainCards;
    private JLabel statusLabel;
    private final List<JButton> navButtons = new ArrayList<>();

    // Tổng quan
    private JPanel dashboardBody;

    // Tài khoản
    private final List<AccountSummaryView> accounts = new ArrayList<>();
    private JPanel accountStatsPanel;
    private JPanel accountRowsPanel;
    private JLabel accountRangeLabel;
    private JTextField accountSearchField;
    private JComboBox<String> accountStatusFilter;
    private JComboBox<String> accountRoleFilter;
    private int accountPage = 1;
    private static final int ACCOUNT_PAGE_SIZE = 8;
    private static final double[] ACCOUNT_COLUMN_WEIGHTS = {0.27, 0.25, 0.12, 0.20, 0.10, 0.06};

    // Dự án
    private JTable projectTable;
    private final List<ProjectView> projects = new ArrayList<>();

    // Công việc
    private JTable taskTable;
    private final List<TaskView> tasks = new ArrayList<>();
    private JComboBox<Option<Long>> taskProjectFilter;
    private boolean updatingTaskFilter;

    // Hồ sơ
    private JTextArea profileInfo;

    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color BLUE_DARK = new Color(29, 78, 216);
    private static final Color GREEN = new Color(5, 150, 105);
    private static final Color ORANGE = new Color(245, 158, 11);
    private static final Color RED = new Color(225, 29, 72);
    private static final Color PURPLE = new Color(124, 58, 237);
    private static final Color SLATE = new Color(100, 116, 139);
    private static final Color BACKGROUND = new Color(245, 247, 251);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color SIDEBAR = new Color(15, 23, 42);
    private static final Color SIDEBAR_ACTIVE = new Color(37, 99, 235);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LIGHT_BUTTON = new Color(226, 232, 240);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 2026;
        SwingUtilities.invokeLater(() -> {
            try {
                configureUi();
                TaskSwingClient client = new TaskSwingClient(new ServerConnection(host, port));
                client.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Không kết nối được server " + host + ":" + port
                        + "\n" + e.getMessage(), "Lỗi khởi động", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void configureUi() throws Exception {
        try {
            Class<?> laf = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            laf.getMethod("setup").invoke(null);
        } catch (ReflectiveOperationException e) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 18);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.selectionBackground", new Color(219, 234, 254));
        UIManager.put("Table.selectionForeground", NAVY);
        UIManager.put("Button.font", BODY_FONT);
        UIManager.put("Label.font", BODY_FONT);
        UIManager.put("TextField.font", BODY_FONT);
        UIManager.put("PasswordField.font", BODY_FONT);
        UIManager.put("Table.font", BODY_FONT);
        UIManager.put("TableHeader.font", LABEL_FONT);
        UIManager.put("OptionPane.messageFont", BODY_FONT);
        UIManager.put("OptionPane.buttonFont", BODY_FONT);
    }

    public TaskSwingClient(ServerConnection connection) {
        this.connection = connection;
        setTitle("Quản lý phân công công việc");
        setSize(1240, 760);
        setMinimumSize(new Dimension(1080, 660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ImageIcon appLogo = logoIcon(48);
        if (appLogo != null) {
            setIconImage(appLogo.getImage());
        }
        showLoginScreen();
    }

    // ================================================================= gọi server (SwingWorker)

    /** Gửi yêu cầu trên luồng nền; thành công thì gọi onSuccess trên EDT, lỗi thì hiện thông báo. */
    private void call(Action action, Map<String, Serializable> data, Consumer<Response> onSuccess) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                return connection.send(new Request(action, token, data));
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                Response response;
                try {
                    response = get();
                } catch (Exception e) {
                    response = Response.error("Lỗi kết nối: " + e.getMessage());
                }
                if (response.isSessionExpired()) {
                    error(response.getMessage());
                    token = null;
                    currentUser = null;
                    showLoginScreen();
                    return;
                }
                if (!response.isSuccess()) {
                    error(response.getMessage());
                    return;
                }
                if (onSuccess != null) {
                    onSuccess.accept(response);
                }
            }
        }.execute();
    }

    private void call(Action action, Consumer<Response> onSuccess) {
        call(action, Map.of(), onSuccess);
    }

    /** Gửi yêu cầu thay đổi dữ liệu: thành công thì báo, rồi chạy after (thường là tải lại dữ liệu). */
    private void submit(Action action, Map<String, Serializable> data, Runnable after) {
        call(action, data, response -> {
            info(response.getMessage());
            if (after != null) {
                after.run();
            }
        });
    }

    // ================================================================= đăng nhập / đăng ký

    private void showLoginScreen() {
        JPanel root = new GradientPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(54, 72, 54, 72));

        JPanel hero = transparentPanel(new GridBagLayout());
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.weightx = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, 42);
        hero.add(loginIntroPanel(), left);

        JPanel card = cardPanel(new BorderLayout(0, 18), 28);
        card.setPreferredSize(new Dimension(400, 450));
        JLabel title = new JLabel("Đăng nhập hệ thống");
        title.setFont(TITLE_FONT);
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Server: " + connection.address());
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(MUTED);
        JPanel heading = transparentPanel(new BorderLayout(0, 6));
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.CENTER);

        JPanel form = transparentPanel(new GridBagLayout());
        JTextField username = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        addField(form, 0, "Tên đăng nhập", username);
        addField(form, 1, "Mật khẩu", password);

        JPanel buttons = transparentPanel(new GridBagLayout());
        JButton loginButton = button("Đăng nhập", BLUE);
        JButton registerButton = button("Nhân viên mới? Đăng ký tài khoản", NAVY);
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.weightx = 1;
        rule.insets = new Insets(0, 0, 10, 0);
        buttons.add(loginButton, rule);
        rule.gridy = 1;
        rule.insets = new Insets(0, 0, 0, 0);
        buttons.add(registerButton, rule);

        JLabel hint = new JLabel("Admin mặc định: admin / admin123", JLabel.CENTER);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(MUTED);

        JPanel south = transparentPanel(new BorderLayout(0, 12));
        south.add(buttons, BorderLayout.CENTER);
        south.add(hint, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        hero.add(card, right);
        root.add(hero, BorderLayout.CENTER);

        Runnable doLogin = () -> {
            token = null;
            call(Action.LOGIN, map("username", username.getText(), "password", new String(password.getPassword())),
                    response -> {
                        LoginResult result = (LoginResult) response.getData();
                        token = result.token();
                        currentUser = result.employee();
                        showMainScreen();
                    });
        };
        loginButton.addActionListener(e -> doLogin.run());
        password.addActionListener(e -> doLogin.run());
        registerButton.addActionListener(e -> showRegisterDialog());
        getRootPane().setDefaultButton(loginButton);

        setContentPane(root);
        revalidate();
        repaint();
    }

    private void showRegisterDialog() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField fullName = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JPanel panel = formPanel(new String[]{"Tên đăng nhập", "Mật khẩu (≥ 6 ký tự)", "Họ tên", "Email", "Số điện thoại"},
                new Component[]{username, password, fullName, email, phone});
        panel.add(noteLabel("Tài khoản sẽ ở trạng thái \"Chờ duyệt\" cho tới khi quản lý duyệt."), fullRow(5));
        if (confirm(panel, "Đăng ký tài khoản nhân viên mới")) {
            token = null;
            submit(Action.REGISTER, map(
                    "username", username.getText(),
                    "password", new String(password.getPassword()),
                    "fullName", fullName.getText(),
                    "email", email.getText(),
                    "phone", phone.getText()), null);
        }
    }

    // ================================================================= khung chính

    private boolean isStaff() {
        return currentUser != null && currentUser.role() != Role.EMPLOYEE;
    }

    private boolean isAdmin() {
        return currentUser != null && currentUser.role() == Role.ADMIN;
    }

    private void showMainScreen() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(270, 0));
        sidebar.setBorder(new EmptyBorder(24, 18, 18, 18));
        sidebar.add(brandPanel());
        sidebar.add(Box.createVerticalStrut(26));

        cardLayout = new CardLayout();
        mainCards = new JPanel(cardLayout);
        mainCards.setBackground(BACKGROUND);
        navButtons.clear();

        addNavigation(sidebar, "Tổng quan", "dashboard", dashboardPanel(), true, this::refreshDashboard);
        if (isStaff()) {
            addNavigation(sidebar, isAdmin() ? "Tài khoản" : "Nhân viên", "accounts", accountPanel(), false,
                    this::refreshAccounts);
            addNavigation(sidebar, isAdmin() ? "Tất cả dự án" : "Dự án phụ trách", "projects", projectPanel(), false,
                    this::refreshProjects);
            addNavigation(sidebar, "Công việc", "tasks", taskPanel(), false, this::refreshTasks);
        } else {
            addNavigation(sidebar, "Công việc của tôi", "tasks", taskPanel(), false, this::refreshTasks);
            addNavigation(sidebar, "Dự án của tôi", "projects", projectPanel(), false, this::refreshProjects);
        }
        addNavigation(sidebar, "Hồ sơ", "profile", profilePanel(), false, this::refreshProfile);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(userCard());

        root.add(sidebar, BorderLayout.WEST);
        root.add(mainCards, BorderLayout.CENTER);
        setContentPane(root);
        revalidate();
        repaint();

        refreshDashboard();
        refreshProjects();
        refreshTasks();
        if (isStaff()) {
            refreshAccounts();
        }
    }

    private JPanel userCard() {
        JPanel userCard = new RoundedPanel(new BorderLayout(10, 10), new Color(30, 41, 59), new Color(51, 65, 85), 18);
        userCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        userCard.setPreferredSize(new Dimension(234, 120));
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(Color.WHITE);
        updateUserLabel();
        JPanel userInfo = transparentPanel(new BorderLayout(10, 0));
        userInfo.add(new AvatarLabel(initials(currentUser.fullName()), roleColor(currentUser.role())), BorderLayout.WEST);
        userInfo.add(statusLabel, BorderLayout.CENTER);
        JButton logout = button("Đăng xuất", RED);
        logout.addActionListener(e -> logout());
        userCard.add(userInfo, BorderLayout.CENTER);
        userCard.add(logout, BorderLayout.SOUTH);
        return userCard;
    }

    private void addNavigation(JPanel sidebar, String label, String key, JPanel panel, boolean selected, Runnable onOpen) {
        JButton nav = new ModernButton("•  " + label);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        nav.setPreferredSize(new Dimension(226, 46));
        nav.putClientProperty("navKey", key);
        navButtons.add(nav);
        styleNavButton(nav, selected);
        nav.setHorizontalAlignment(JButton.LEFT);
        nav.setBorder(new EmptyBorder(12, 16, 12, 16));
        nav.addActionListener(e -> {
            cardLayout.show(mainCards, key);
            updateNavigation(key);
            onOpen.run();
        });
        sidebar.add(nav);
        sidebar.add(Box.createVerticalStrut(8));
        mainCards.add(panel, key);
        if (selected) {
            cardLayout.show(mainCards, key);
        }
    }

    private void updateNavigation(String key) {
        for (JButton nav : navButtons) {
            styleNavButton(nav, key.equals(nav.getClientProperty("navKey")));
        }
    }

    private static void styleNavButton(JButton nav, boolean selected) {
        styleButton(nav, selected ? SIDEBAR_ACTIVE : SIDEBAR);
        nav.setForeground(selected ? Color.WHITE : new Color(203, 213, 225));
    }

    private void updateUserLabel() {
        if (statusLabel != null && currentUser != null) {
            statusLabel.setText("<html>" + escape(currentUser.fullName())
                    + "<br><span style='font-size:10px;color:#cbd5e1'>" + currentUser.role().label()
                    + " • @" + escape(currentUser.username()) + "</span></html>");
        }
    }

    private void logout() {
        call(Action.LOGOUT, response -> {
            token = null;
            currentUser = null;
            showLoginScreen();
        });
    }

    // ================================================================= tổng quan

    private JPanel dashboardPanel() {
        JPanel panel = contentPanel();
        String subtitle = switch (currentUser.role()) {
            case ADMIN -> "Toàn bộ dự án, công việc và nhân sự của công ty.";
            case MANAGER -> "Các dự án bạn phụ trách và tiến độ công việc.";
            case EMPLOYEE -> "Công việc được giao và các dự án bạn tham gia.";
        };
        panel.add(pageHeader("Tổng quan", subtitle), BorderLayout.NORTH);
        dashboardBody = transparentPanel(new BorderLayout(0, 16));
        panel.add(dashboardBody, BorderLayout.CENTER);
        return panel;
    }

    private void refreshDashboard() {
        call(Action.DASHBOARD, response -> renderDashboard((DashboardStats) response.getData()));
    }

    private void renderDashboard(DashboardStats data) {
        dashboardBody.removeAll();
        JPanel stats = transparentPanel(new GridLayout(1, 4, 14, 0));
        stats.add(statCard(isStaff() ? "Dự án" : "Dự án tham gia", String.valueOf(data.totalProjects()),
                isStaff() ? "Nhân viên tham gia: " + data.totalEmployees() : "Đồng đội: " + data.totalEmployees(), BLUE));
        stats.add(statCard("Tổng công việc", String.valueOf(data.totalTasks()),
                "Hoàn thành " + data.progress() + "%", GREEN));
        stats.add(statCard("Đang thực hiện", String.valueOf(data.inProgressTasks()),
                "Tạm hoãn: " + data.onHoldTasks(), ORANGE));
        stats.add(statCard("Quá hạn", String.valueOf(data.overdueTasks()), "Cần xử lý ngay", RED));
        dashboardBody.add(stats, BorderLayout.NORTH);

        JPanel detail = transparentPanel(new GridBagLayout());
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.weightx = 0.6;
        rule.weighty = 1;
        rule.fill = GridBagConstraints.BOTH;
        rule.insets = new Insets(0, 0, 0, 16);
        JPanel left = transparentPanel(new BorderLayout(0, 16));
        left.add(progressPanel(data), BorderLayout.NORTH);
        left.add(nearDeadlinePanel(data.nearDeadlineTasks()), BorderLayout.CENTER);
        detail.add(left, rule);
        rule.gridx = 1;
        rule.weightx = 0.4;
        rule.insets = new Insets(0, 0, 0, 0);
        detail.add(projectSummaryPanel(data.projects()), rule);
        dashboardBody.add(detail, BorderLayout.CENTER);
        dashboardBody.revalidate();
        dashboardBody.repaint();
    }

    private JPanel progressPanel(DashboardStats data) {
        JPanel panel = cardPanel(new BorderLayout(0, 14), 20);
        JProgressBar progress = progressBar(data.progress());
        JLabel percent = new JLabel(data.progress() + "% công việc đã hoàn thành");
        percent.setFont(new Font("Segoe UI", Font.BOLD, 20));
        percent.setForeground(NAVY);
        JPanel top = transparentPanel(new BorderLayout(0, 8));
        top.add(sectionTitle("Tiến độ chung"), BorderLayout.NORTH);
        top.add(percent, BorderLayout.CENTER);
        top.add(progress, BorderLayout.SOUTH);
        JPanel counters = transparentPanel(new GridLayout(1, 4, 10, 0));
        counters.add(counterPill("Chưa bắt đầu", data.todoTasks(), BLUE));
        counters.add(counterPill("Đang làm", data.inProgressTasks(), ORANGE));
        counters.add(counterPill("Tạm hoãn", data.onHoldTasks(), SLATE));
        counters.add(counterPill("Hoàn thành", data.completedTasks(), GREEN));
        panel.add(top, BorderLayout.NORTH);
        panel.add(counters, BorderLayout.CENTER);
        return panel;
    }

    private JPanel nearDeadlinePanel(List<TaskView> nearTasks) {
        JPanel panel = cardPanel(new BorderLayout(0, 12), 20);
        panel.add(sectionTitle("Công việc gần deadline"), BorderLayout.NORTH);
        JPanel list = transparentPanel(new GridBagLayout());
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.anchor = GridBagConstraints.NORTH;
        rule.insets = new Insets(0, 0, 8, 0);
        if (nearTasks.isEmpty()) {
            list.add(noteLabel("Không có công việc nào sắp đến hạn."), rule);
        } else {
            int row = 0;
            for (TaskView task : nearTasks) {
                rule.gridy = row++;
                list.add(deadlineRow(task), rule);
            }
        }
        rule.gridy = 99;
        rule.weighty = 1;
        list.add(Box.createGlue(), rule);
        panel.add(plainScroll(list), BorderLayout.CENTER);
        return panel;
    }

    private static JScrollPane plainScroll(JPanel content) {
        WidthTrackingPanel wrapper = new WidthTrackingPanel();
        wrapper.add(content, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel projectSummaryPanel(List<ProjectView> projectViews) {
        JPanel panel = cardPanel(new BorderLayout(0, 12), 20);
        panel.add(sectionTitle(isStaff() ? "Dự án" : "Dự án của tôi"), BorderLayout.NORTH);
        JPanel list = transparentPanel(new GridBagLayout());
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.insets = new Insets(0, 0, 10, 0);
        if (projectViews.isEmpty()) {
            list.add(noteLabel(isStaff() ? "Chưa có dự án nào." : "Bạn chưa tham gia dự án nào."), rule);
        }
        int row = 0;
        for (ProjectView project : projectViews.stream().limit(6).toList()) {
            rule.gridy = row++;
            JPanel item = new RoundedPanel(new BorderLayout(0, 6), new Color(248, 250, 252), BORDER, 16);
            item.setBorder(new EmptyBorder(10, 12, 10, 12));
            JLabel name = new JLabel(project.name());
            name.setFont(new Font("Segoe UI", Font.BOLD, 13));
            name.setForeground(NAVY);
            JPanel top = transparentPanel(new BorderLayout(8, 0));
            top.add(name, BorderLayout.CENTER);
            top.add(badge(project.status().label(), projectStatusColor(project.status())), BorderLayout.EAST);
            JLabel meta = new JLabel("QL: " + nullSafe(project.managerName()) + "  •  " + project.completedTasks()
                    + "/" + project.totalTasks() + " việc  •  " + project.progress() + "%");
            meta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            meta.setForeground(MUTED);
            item.add(top, BorderLayout.NORTH);
            item.add(meta, BorderLayout.CENTER);
            item.add(progressBar(project.progress()), BorderLayout.SOUTH);
            list.add(item, rule);
        }
        rule.gridy = 99;
        rule.weighty = 1;
        list.add(Box.createGlue(), rule);
        panel.add(plainScroll(list), BorderLayout.CENTER);
        return panel;
    }

    private static JPanel deadlineRow(TaskView task) {
        JPanel row = new RoundedPanel(new BorderLayout(12, 0), new Color(248, 250, 252), BORDER, 16);
        row.setBorder(new EmptyBorder(10, 14, 10, 14));
        JPanel text = transparentPanel(new BorderLayout(0, 4));
        JLabel title = new JLabel(task.title());
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(NAVY);
        JLabel meta = new JLabel(task.projectName() + "  •  " + nullSafe(task.assigneeName())
                + "  •  Deadline: " + display(task.deadline()));
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        meta.setForeground(MUTED);
        text.add(title, BorderLayout.NORTH);
        text.add(meta, BorderLayout.CENTER);
        JPanel badges = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.add(badge(task.priority().label(), priorityColor(task.priority())));
        badges.add(badge(task.overdue() ? "Quá hạn" : task.status().label(),
                task.overdue() ? RED : taskStatusColor(task.status())));
        row.add(text, BorderLayout.CENTER);
        row.add(badges, BorderLayout.EAST);
        return row;
    }

    // ================================================================= tài khoản

    private JPanel accountPanel() {
        JPanel panel = contentPanel();
        panel.add(pageHeader(isAdmin() ? "Quản lý tài khoản" : "Quản lý nhân viên",
                isAdmin() ? "Tạo, duyệt, tạm dừng, khóa và xóa tài khoản quản lý / nhân viên."
                        : "Duyệt đăng ký, tạm dừng, khóa và xóa tài khoản nhân viên."), BorderLayout.NORTH);

        JPanel body = transparentPanel(new BorderLayout(0, 14));
        body.add(accountToolbar(), BorderLayout.NORTH);

        JPanel tableCard = cardPanel(new BorderLayout(0, 12), 16);
        accountStatsPanel = transparentPanel(new GridLayout(1, 5, 10, 0));
        accountRowsPanel = transparentPanel(new GridBagLayout());
        accountRangeLabel = new JLabel(" ");
        accountRangeLabel.setForeground(MUTED);

        JPanel footer = transparentPanel(new BorderLayout(12, 0));
        JPanel pages = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton previous = button("< Trước", LIGHT_BUTTON);
        JButton next = button("Sau >", LIGHT_BUTTON);
        previous.addActionListener(e -> {
            if (accountPage > 1) {
                accountPage--;
                renderAccounts();
            }
        });
        next.addActionListener(e -> {
            int maxPage = Math.max(1, (int) Math.ceil(filteredAccounts().size() / (double) ACCOUNT_PAGE_SIZE));
            if (accountPage < maxPage) {
                accountPage++;
                renderAccounts();
            }
        });
        pages.add(previous);
        pages.add(next);
        footer.add(accountRangeLabel, BorderLayout.WEST);
        footer.add(pages, BorderLayout.EAST);

        WidthTrackingPanel rowsWrapper = new WidthTrackingPanel();
        rowsWrapper.add(accountRowsPanel, BorderLayout.CENTER);
        JScrollPane rowsScroll = new JScrollPane(rowsWrapper);
        rowsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rowsScroll.setBorder(null);
        rowsScroll.getViewport().setBackground(CARD);
        rowsScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableCard.add(accountStatsPanel, BorderLayout.NORTH);
        tableCard.add(rowsScroll, BorderLayout.CENTER);
        tableCard.add(footer, BorderLayout.SOUTH);
        body.add(tableCard, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel accountToolbar() {
        JPanel toolbar = cardPanel(new GridBagLayout(), 12);
        accountSearchField = new JTextField();
        accountSearchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên, username, email...");
        styleInput(accountSearchField);
        List<String> statusOptions = new ArrayList<>(List.of("Tất cả trạng thái"));
        for (AccountStatus status : AccountStatus.values()) {
            statusOptions.add(status.label());
        }
        accountStatusFilter = new JComboBox<>(statusOptions.toArray(new String[0]));
        accountRoleFilter = new JComboBox<>(new String[]{"Tất cả vai trò", Role.ADMIN.label(), Role.MANAGER.label(),
                Role.EMPLOYEE.label()});
        JButton refresh = button("Tải lại", LIGHT_BUTTON);
        JButton add = button(isAdmin() ? "+ Thêm tài khoản" : "+ Thêm nhân viên", BLUE);
        refresh.addActionListener(e -> refreshAccounts());
        add.addActionListener(e -> createAccountDialog());

        accountSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetAccountPage();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetAccountPage();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetAccountPage();
            }
        });
        accountStatusFilter.addActionListener(e -> resetAccountPage());
        accountRoleFilter.addActionListener(e -> resetAccountPage());

        GridBagConstraints rule = new GridBagConstraints();
        rule.gridy = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.insets = new Insets(0, 0, 0, 10);
        toolbar.add(accountSearchField, rule);
        rule.weightx = 0;
        rule.gridx = 1;
        toolbar.add(accountStatusFilter, rule);
        if (isAdmin()) {
            rule.gridx = 2;
            toolbar.add(accountRoleFilter, rule);
        }
        rule.gridx = 3;
        toolbar.add(refresh, rule);
        rule.gridx = 4;
        rule.insets = new Insets(0, 0, 0, 0);
        toolbar.add(add, rule);
        return toolbar;
    }

    private void refreshAccounts() {
        if (!isStaff()) {
            return;
        }
        call(Action.LIST_ACCOUNTS, response -> {
            accounts.clear();
            accounts.addAll(castList(response.getData()));
            renderAccounts();
        });
    }

    private void resetAccountPage() {
        accountPage = 1;
        renderAccounts();
    }

    private List<AccountSummaryView> filteredAccounts() {
        String keyword = accountSearchField == null ? "" : accountSearchField.getText().trim().toLowerCase();
        String status = accountStatusFilter == null ? "" : String.valueOf(accountStatusFilter.getSelectedItem());
        String role = accountRoleFilter == null ? "" : String.valueOf(accountRoleFilter.getSelectedItem());
        return accounts.stream().filter(row -> {
            EmployeeView e = row.employee();
            String haystack = (e.fullName() + " " + e.username() + " " + nullToEmpty(e.email())).toLowerCase();
            return (keyword.isBlank() || haystack.contains(keyword))
                    && (status.startsWith("Tất cả") || e.status().label().equals(status))
                    && (role.startsWith("Tất cả") || e.role().label().equals(role));
        }).toList();
    }

    private void renderAccounts() {
        if (accountRowsPanel == null) {
            return;
        }
        accountStatsPanel.removeAll();
        accountStatsPanel.add(miniStat("Tổng", accounts.size(), BLUE));
        accountStatsPanel.add(miniStat("Hoạt động", countStatus(AccountStatus.ACTIVE), GREEN));
        accountStatsPanel.add(miniStat("Chờ duyệt", countStatus(AccountStatus.PENDING), ORANGE));
        accountStatsPanel.add(miniStat("Tạm dừng", countStatus(AccountStatus.ON_LEAVE), SLATE));
        accountStatsPanel.add(miniStat("Khóa / Nghỉ việc",
                countStatus(AccountStatus.LOCKED) + countStatus(AccountStatus.RESIGNED), RED));

        accountRowsPanel.removeAll();
        List<AccountSummaryView> filtered = filteredAccounts();
        int total = filtered.size();
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) ACCOUNT_PAGE_SIZE));
        accountPage = Math.min(accountPage, maxPage);
        int from = total == 0 ? 0 : (accountPage - 1) * ACCOUNT_PAGE_SIZE;
        int to = Math.min(total, from + ACCOUNT_PAGE_SIZE);

        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.insets = new Insets(0, 0, 8, 0);
        if (total == 0) {
            accountRowsPanel.add(noteLabel("Không có tài khoản phù hợp."), rule);
            accountRangeLabel.setText("Hiển thị 0 / 0 tài khoản");
        } else {
            accountRowsPanel.add(accountHeaderRow(), rule);
            int gridY = 1;
            for (AccountSummaryView row : filtered.subList(from, to)) {
                rule.gridy = gridY++;
                accountRowsPanel.add(accountRow(row), rule);
            }
            accountRangeLabel.setText("Hiển thị " + (from + 1) + "-" + to + " / " + total + " tài khoản");
        }
        rule.gridy = 999;
        rule.weighty = 1;
        accountRowsPanel.add(Box.createGlue(), rule);
        accountStatsPanel.revalidate();
        accountRowsPanel.revalidate();
        accountRowsPanel.repaint();
    }

    private long countStatus(AccountStatus status) {
        return accounts.stream().filter(row -> row.employee().status() == status).count();
    }

    private JPanel accountHeaderRow() {
        JPanel header = transparentPanel(new GridBagLayout());
        header.setBorder(new EmptyBorder(0, 12, 0, 12));
        String[] titles = {"Tài khoản", "Liên hệ", "Vai trò", "Trạng thái", "Công việc", "Thao tác"};
        for (int i = 0; i < titles.length; i++) {
            JLabel label = new JLabel(titles[i]);
            label.setFont(LABEL_FONT);
            label.setForeground(MUTED);
            if (i == 5) {
                label.setPreferredSize(new Dimension(52, label.getPreferredSize().height));
            }
            addCell(header, i, label, ACCOUNT_COLUMN_WEIGHTS[i]);
        }
        return header;
    }

    private JPanel accountRow(AccountSummaryView row) {
        EmployeeView employee = row.employee();
        JPanel panel = new RoundedPanel(new GridBagLayout(), Color.WHITE, BORDER, 14);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));
        addCell(panel, 0, identityBlock(employee), ACCOUNT_COLUMN_WEIGHTS[0]);
        addCell(panel, 1, twoLines(nullSafe(employee.email()), nullSafe(employee.phone()), NAVY), ACCOUNT_COLUMN_WEIGHTS[1]);
        addCell(panel, 2, badge(employee.role().label(), roleColor(employee.role())), ACCOUNT_COLUMN_WEIGHTS[2]);
        addCell(panel, 3, statusBlock(employee), ACCOUNT_COLUMN_WEIGHTS[3]);
        String tasksLine = employee.role() == Role.EMPLOYEE
                ? row.openTasks() + " đang làm" : row.managedProjects() + " dự án";
        String tasksLine2 = employee.role() == Role.EMPLOYEE ? row.completedTasks() + " hoàn thành" : "phụ trách";
        addCell(panel, 4, twoLines(tasksLine, tasksLine2, ORANGE.darker()), ACCOUNT_COLUMN_WEIGHTS[4]);
        JButton menu = button("•••", LIGHT_BUTTON);
        menu.setToolTipText("Thao tác");
        menu.setPreferredSize(new Dimension(52, 34));
        menu.addActionListener(e -> accountMenu(menu, row));
        addCell(panel, 5, menu, ACCOUNT_COLUMN_WEIGHTS[5]);
        return panel;
    }

    private JPanel statusBlock(EmployeeView employee) {
        JPanel panel = transparentPanel(new BorderLayout(0, 3));
        JPanel badgeRow = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.add(badge(employee.status().label(), accountStatusColor(employee.status())));
        panel.add(badgeRow, BorderLayout.NORTH);
        String detail = switch (employee.status()) {
            case ON_LEAVE -> nullToEmpty(employee.statusReason())
                    + (employee.statusUntil() == null ? "" : " → " + employee.statusUntil());
            case LOCKED, RESIGNED -> nullToEmpty(employee.statusReason());
            default -> "";
        };
        if (!detail.isBlank()) {
            JLabel label = new JLabel(detail);
            label.setToolTipText(detail);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            label.setForeground(MUTED);
            panel.add(label, BorderLayout.CENTER);
        }
        return panel;
    }

    /** Client chỉ hiện thao tác hợp lệ; server vẫn kiểm tra lại quyền. */
    private boolean canManage(EmployeeView target) {
        if (currentUser == null || target.id().equals(currentUser.id())) {
            return false;
        }
        if (isAdmin()) {
            return target.role() != Role.ADMIN;
        }
        return currentUser.role() == Role.MANAGER && target.role() == Role.EMPLOYEE;
    }

    private void accountMenu(JButton owner, AccountSummaryView row) {
        EmployeeView target = row.employee();
        JPopupMenu menu = new JPopupMenu();
        menuItem(menu, "Xem chi tiết & lịch sử", () -> showAccountDetail(row));
        if (canManage(target)) {
            switch (target.status()) {
                case PENDING -> {
                    menuItem(menu, "Duyệt tài khoản", () -> confirmThen("Duyệt tài khoản của " + target.fullName()
                                    + "?\nNhân viên sẽ đăng nhập được và có thể được giao việc.", "Duyệt",
                            () -> submit(Action.APPROVE_ACCOUNT, map("employeeId", target.id()), this::refreshAccounts)));
                    menuItem(menu, "Từ chối đăng ký", () -> confirmThen("Từ chối đăng ký của " + target.fullName()
                                    + "?\nBản đăng ký sẽ bị xóa khỏi hệ thống.", "Từ chối",
                            () -> submit(Action.REJECT_ACCOUNT, map("employeeId", target.id()), this::refreshAccounts)));
                }
                case ACTIVE -> {
                    menuItem(menu, "Chỉnh sửa thông tin", () -> editAccountDialog(target));
                    menuItem(menu, "Tạm dừng (nghỉ dài ngày)", () -> suspendDialog(row));
                    if (row.openTasks() > 0) {
                        menuItem(menu, "Chuyển giao công việc", () -> transferOpenTasksDialog(row));
                    }
                    menuItem(menu, "Khóa tài khoản", () -> lockDialog(target));
                    menu.addSeparator();
                    menuItem(menu, "Xóa tài khoản", () -> deleteAccountDialog(row));
                }
                case ON_LEAVE -> {
                    menuItem(menu, "Kích hoạt lại (quay lại làm việc)", () -> confirmThen("Kích hoạt lại tài khoản của "
                                    + target.fullName() + "?\nCác công việc đang tạm hoãn sẽ được tiếp tục.", "Kích hoạt",
                            () -> submit(Action.REACTIVATE_ACCOUNT, map("employeeId", target.id()), this::refreshAfterAccountChange)));
                    menuItem(menu, "Chỉnh sửa thông tin", () -> editAccountDialog(target));
                    if (row.openTasks() > 0) {
                        menuItem(menu, "Chuyển giao công việc", () -> transferOpenTasksDialog(row));
                    }
                    menuItem(menu, "Khóa tài khoản", () -> lockDialog(target));
                    menu.addSeparator();
                    menuItem(menu, "Xóa tài khoản", () -> deleteAccountDialog(row));
                }
                case LOCKED -> {
                    menuItem(menu, "Mở khóa", () -> confirmThen("Mở khóa tài khoản của " + target.fullName() + "?",
                            "Mở khóa", () -> submit(Action.UNLOCK_ACCOUNT, map("employeeId", target.id()), this::refreshAccounts)));
                    menuItem(menu, "Chỉnh sửa thông tin", () -> editAccountDialog(target));
                    if (row.openTasks() > 0) {
                        menuItem(menu, "Chuyển giao công việc", () -> transferOpenTasksDialog(row));
                    }
                    menu.addSeparator();
                    menuItem(menu, "Xóa tài khoản", () -> deleteAccountDialog(row));
                }
                case RESIGNED -> {
                    // Tài khoản đã nghỉ việc: chỉ xem lịch sử
                }
            }
        }
        menu.show(owner, 0, owner.getHeight());
    }

    private void refreshAfterAccountChange() {
        refreshAccounts();
        refreshTasks();
        refreshProjects();
    }

    private void createAccountDialog() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField fullName = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JComboBox<Option<Role>> role = new JComboBox<>();
        role.addItem(new Option<>(Role.EMPLOYEE, Role.EMPLOYEE.label()));
        if (isAdmin()) {
            role.addItem(new Option<>(Role.MANAGER, Role.MANAGER.label()));
        }
        JPanel panel = formPanel(new String[]{"Vai trò", "Tên đăng nhập", "Mật khẩu ban đầu", "Họ tên", "Email", "Số điện thoại"},
                new Component[]{role, username, password, fullName, email, phone});
        role.setEnabled(isAdmin());
        if (confirm(panel, isAdmin() ? "Thêm tài khoản" : "Thêm nhân viên")) {
            submit(Action.CREATE_ACCOUNT, map(
                    "role", selected(role),
                    "username", username.getText(),
                    "password", new String(password.getPassword()),
                    "fullName", fullName.getText(),
                    "email", email.getText(),
                    "phone", phone.getText()), this::refreshAccounts);
        }
    }

    private void editAccountDialog(EmployeeView target) {
        JTextField fullName = new JTextField(target.fullName());
        JTextField email = new JTextField(nullToEmpty(target.email()));
        JTextField phone = new JTextField(nullToEmpty(target.phone()));
        JComboBox<Option<Role>> role = new JComboBox<>();
        role.addItem(new Option<>(Role.EMPLOYEE, Role.EMPLOYEE.label()));
        role.addItem(new Option<>(Role.MANAGER, Role.MANAGER.label()));
        selectOption(role, target.role());
        role.setEnabled(isAdmin());
        JPanel panel = formPanel(new String[]{"Họ tên", "Email", "Số điện thoại", "Vai trò"},
                new Component[]{fullName, email, phone, role});
        if (!isAdmin()) {
            panel.add(noteLabel("Chỉ Admin được thay đổi vai trò."), fullRow(4));
        }
        if (confirm(panel, "Chỉnh sửa: " + target.fullName())) {
            submit(Action.UPDATE_ACCOUNT, map(
                    "employeeId", target.id(),
                    "fullName", fullName.getText(),
                    "email", email.getText(),
                    "phone", phone.getText(),
                    "role", isAdmin() ? selected(role) : null), this::refreshAccounts);
        }
    }

    private void lockDialog(EmployeeView target) {
        JTextArea reason = new JTextArea(3, 26);
        JPanel panel = formPanel(new String[]{"Lý do khóa *"}, new Component[]{new JScrollPane(reason)});
        panel.add(noteLabel("Ví dụ: vi phạm quy định, nghi bị lộ mật khẩu, đang chờ xử lý..."), fullRow(1));
        if (confirm(panel, "Khóa tài khoản: " + target.fullName())) {
            submit(Action.LOCK_ACCOUNT, map("employeeId", target.id(), "reason", reason.getText()), this::refreshAccounts);
        }
    }

    /** Tạm dừng: chọn lý do, thời gian nghỉ và cách xử lý công việc đang làm. */
    private void suspendDialog(AccountSummaryView row) {
        EmployeeView target = row.employee();
        call(Action.LIST_ASSIGNABLE_EMPLOYEES, response -> {
            List<EmployeeView> receivers = this.<EmployeeView>castList(response.getData()).stream()
                    .filter(e -> !e.id().equals(target.id())).toList();
            JComboBox<LeaveReason> reason = new JComboBox<>(LeaveReason.values());
            JTextField from = new JTextField(LocalDate.now().toString());
            JTextField until = new JTextField(LocalDate.now().plusMonths(1).toString());
            JTextArea note = new JTextArea(3, 26);
            JRadioButton hold = new JRadioButton("Giữ lại, tạm hoãn tới khi quay lại", true);
            JRadioButton transfer = new JRadioButton("Chuyển giao cho:");
            hold.setOpaque(false);
            transfer.setOpaque(false);
            ButtonGroup group = new ButtonGroup();
            group.add(hold);
            group.add(transfer);
            JComboBox<Option<Long>> receiver = employeeCombo(receivers);
            receiver.setEnabled(false);
            transfer.addActionListener(e -> receiver.setEnabled(true));
            hold.addActionListener(e -> receiver.setEnabled(false));
            JPanel handling = transparentPanel(new GridLayout(3, 1, 0, 4));
            handling.add(hold);
            handling.add(transfer);
            handling.add(receiver);
            JPanel panel = formPanel(new String[]{"Lý do *", "Từ ngày (yyyy-MM-dd)", "Dự kiến quay lại *", "Ghi chú",
                            "Công việc đang làm"},
                    new Component[]{reason, from, until, new JScrollPane(note), handling});
            panel.add(noteLabel(target.fullName() + " đang có " + row.openTasks()
                    + " công việc chưa hoàn thành. Trong thời gian tạm dừng sẽ không đăng nhập và không được giao việc."), fullRow(5));
            if (!confirm(panel, "Tạm dừng tài khoản: " + target.fullName())) {
                return;
            }
            LocalDate fromDate;
            LocalDate untilDate;
            try {
                fromDate = parseDate(from.getText());
                untilDate = parseDate(until.getText());
            } catch (DateTimeParseException ex) {
                error("Ngày không hợp lệ, hãy nhập theo dạng yyyy-MM-dd.");
                return;
            }
            submit(Action.SUSPEND_ACCOUNT, map(
                    "employeeId", target.id(),
                    "reason", (LeaveReason) reason.getSelectedItem(),
                    "from", fromDate,
                    "until", untilDate,
                    "note", note.getText(),
                    "transferTasks", transfer.isSelected(),
                    "transferToId", transfer.isSelected() ? selected(receiver) : null), this::refreshAfterAccountChange);
        });
    }

    private void transferOpenTasksDialog(AccountSummaryView row) {
        EmployeeView from = row.employee();
        call(Action.LIST_ASSIGNABLE_EMPLOYEES, response -> {
            List<EmployeeView> receivers = this.<EmployeeView>castList(response.getData()).stream()
                    .filter(e -> !e.id().equals(from.id())).toList();
            if (receivers.isEmpty()) {
                error("Không có nhân viên nào đang hoạt động để nhận chuyển giao.");
                return;
            }
            JComboBox<Option<Long>> receiver = employeeCombo(receivers);
            JPanel panel = formPanel(new String[]{"Chuyển cho"}, new Component[]{receiver});
            panel.add(noteLabel("Chuyển toàn bộ " + row.openTasks() + " công việc chưa hoàn thành của "
                    + from.fullName() + ". Người nhận sẽ được thêm vào dự án nếu chưa là thành viên."), fullRow(1));
            if (confirm(panel, "Chuyển giao công việc")) {
                submit(Action.TRANSFER_OPEN_TASKS, map("fromEmployeeId", from.id(), "toEmployeeId", selected(receiver)),
                        this::refreshAfterAccountChange);
            }
        });
    }

    /** Xóa tài khoản: bắt buộc chọn lý do; còn việc chưa xong thì đề nghị chuyển giao trước. */
    private void deleteAccountDialog(AccountSummaryView row) {
        EmployeeView target = row.employee();
        if (row.openTasks() > 0) {
            int choice = JOptionPane.showOptionDialog(this,
                    messageLabel(target.fullName() + " còn " + row.openTasks() + " công việc chưa hoàn thành.\n"
                            + "Cần chuyển giao công việc trước khi xóa tài khoản."),
                    "Chưa thể xóa", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new Object[]{"Đóng", "Chuyển giao ngay"}, "Chuyển giao ngay");
            if (choice == 1) {
                transferOpenTasksDialog(row);
            }
            return;
        }
        JComboBox<RemovalReason> reason = new JComboBox<>(RemovalReason.values());
        JTextArea note = new JTextArea(3, 26);
        JLabel explain = noteLabel(" ");
        Runnable updateExplain = () -> {
            RemovalReason selected = (RemovalReason) reason.getSelectedItem();
            explain.setText(selected != null && selected.isSoftDelete()
                    ? "<html><div style='width:430px'>Tài khoản chuyển sang <b>Đã nghỉ việc</b>: không đăng nhập được, "
                    + "lịch sử " + row.totalTasks() + " công việc vẫn được giữ lại.</div></html>"
                    : "<html><div style='width:430px'>Tài khoản bị <b>xóa hẳn</b> khỏi hệ thống. Chỉ thực hiện được khi "
                    + "tài khoản chưa có công việc nào (hiện có " + row.totalTasks() + ").</div></html>");
        };
        reason.addActionListener(e -> updateExplain.run());
        updateExplain.run();
        JPanel panel = formPanel(new String[]{"Lý do xóa *", "Ghi chú"}, new Component[]{reason, new JScrollPane(note)});
        panel.add(explain, fullRow(2));
        if (confirm(panel, "Xóa tài khoản: " + target.fullName())) {
            submit(Action.DELETE_ACCOUNT, map("employeeId", target.id(), "reason", (RemovalReason) reason.getSelectedItem(),
                    "note", note.getText()), this::refreshAfterAccountChange);
        }
    }

    private void showAccountDetail(AccountSummaryView row) {
        EmployeeView e = row.employee();
        call(Action.LIST_STATUS_HISTORY, map("employeeId", e.id()), response -> {
            List<StatusHistoryView> history = castList(response.getData());
            JPanel panel = new JPanel(new BorderLayout(0, 12));
            panel.add(identityBlock(e), BorderLayout.NORTH);
            JTextArea info = new JTextArea("""
                    Email          : %s
                    Số điện thoại  : %s
                    Vai trò        : %s
                    Trạng thái     : %s %s
                    Công việc      : %d chưa xong, %d hoàn thành, tổng %d
                    """.formatted(nullSafe(e.email()), nullSafe(e.phone()), e.role().label(), e.status().label(),
                    e.statusReason() == null ? "" : "(" + e.statusReason() + ")",
                    row.openTasks(), row.completedTasks(), row.totalTasks()));
            info.setEditable(false);
            info.setOpaque(false);
            info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            DefaultTableModel model = model("Thời gian", "Từ", "Sang", "Lý do", "Thời hạn", "Người thực hiện");
            for (StatusHistoryView h : history) {
                String period = h.toDate() == null ? display(h.fromDate()) : h.fromDate() + " → " + h.toDate();
                model.addRow(new Object[]{h.changedAt().toString().replace('T', ' ').substring(0, 16),
                        h.fromStatus() == null ? "(mới tạo)" : h.fromStatus().label(), h.toStatus().label(),
                        nullToEmpty(h.reason()) + (h.note() == null || h.note().isBlank() ? "" : " - " + h.note()),
                        period, nullToEmpty(h.changedByName())});
            }
            JTable table = table();
            table.setModel(model);
            JScrollPane scroll = styledScroll(table);
            scroll.setPreferredSize(new Dimension(820, 220));
            JPanel historyPanel = transparentPanel(new BorderLayout(0, 6));
            historyPanel.add(sectionTitle("Lịch sử trạng thái"), BorderLayout.NORTH);
            historyPanel.add(scroll, BorderLayout.CENTER);
            panel.add(info, BorderLayout.CENTER);
            panel.add(historyPanel, BorderLayout.SOUTH);
            JOptionPane.showMessageDialog(this, panel, "Chi tiết tài khoản", JOptionPane.PLAIN_MESSAGE);
        });
    }

    // ================================================================= dự án

    private JPanel projectPanel() {
        projectTable = table();
        JPanel actions = toolbarPanel();
        JButton refresh = button("Tải lại", LIGHT_BUTTON);
        refresh.addActionListener(e -> refreshProjects());
        actions.add(refresh);
        JButton detail = button("Xem chi tiết", BLUE);
        detail.addActionListener(e -> withSelectedProject(this::showProjectDetail));
        actions.add(detail);
        if (isStaff()) {
            JButton create = button("+ Tạo dự án", GREEN);
            JButton edit = button("Sửa / đổi trạng thái", NAVY);
            JButton members = button("Thành viên", BLUE_DARK);
            JButton delete = button("Xóa dự án", RED);
            create.addActionListener(e -> createProjectDialog());
            edit.addActionListener(e -> withSelectedProject(this::editProjectDialog));
            members.addActionListener(e -> withSelectedProject(this::membersDialog));
            delete.addActionListener(e -> withSelectedProject(this::deleteProject));
            actions.add(create);
            actions.add(edit);
            actions.add(members);
            actions.add(delete);
        }
        String title = isAdmin() ? "Tất cả dự án" : isStaff() ? "Dự án phụ trách" : "Dự án của tôi";
        String subtitle = isAdmin() ? "Admin quản lý mọi dự án và chọn quản lý phụ trách."
                : isStaff() ? "Đăng, sửa, xóa dự án, phân công thành viên và cập nhật trạng thái."
                : "Các dự án bạn đang là thành viên.";
        return pageShell(title, subtitle, actions, styledScroll(projectTable));
    }

    private void refreshProjects() {
        call(Action.LIST_PROJECTS, response -> {
            projects.clear();
            projects.addAll(castList(response.getData()));
            DefaultTableModel model = model("ID", "Tên dự án", "Quản lý phụ trách", "Bắt đầu", "Deadline",
                    "Trạng thái", "Thành viên", "Công việc", "Tiến độ");
            for (ProjectView p : projects) {
                model.addRow(new Object[]{p.id(), p.name(), p.managerName(), display(p.startDate()),
                        display(p.deadline()), p.status().label(), p.members().size(),
                        p.completedTasks() + "/" + p.totalTasks(), p.progress() + "%"});
            }
            projectTable.setModel(model);
            setColumnWidths(projectTable, 40, 260, 150, 92, 92, 120, 80, 80, 66);
            updateTaskProjectFilter();
        });
    }

    private void withSelectedProject(Consumer<ProjectView> action) {
        Long id = selectedId(projectTable);
        if (id == null) {
            info("Hãy chọn một dự án trong bảng.");
            return;
        }
        projects.stream().filter(p -> p.id().equals(id)).findFirst().ifPresent(action);
    }

    private void createProjectDialog() {
        if (isAdmin()) {
            call(Action.LIST_MANAGERS, response -> projectDialog(null, castList(response.getData())));
        } else {
            projectDialog(null, List.of());
        }
    }

    private void editProjectDialog(ProjectView project) {
        if (isAdmin()) {
            call(Action.LIST_MANAGERS, response -> projectDialog(project, castList(response.getData())));
        } else {
            projectDialog(project, List.of());
        }
    }

    /** Form dự án dùng chung cho tạo mới (project == null) và sửa. */
    private void projectDialog(ProjectView project, List<EmployeeView> managers) {
        boolean editing = project != null;
        JTextField name = new JTextField(editing ? project.name() : "");
        JTextArea description = new JTextArea(editing ? nullToEmpty(project.description()) : "", 4, 26);
        JTextField start = new JTextField(editing ? dateText(project.startDate()) : LocalDate.now().toString());
        JTextField deadline = new JTextField(editing ? dateText(project.deadline()) : "");
        JComboBox<Option<ProjectStatus>> status = new JComboBox<>();
        for (ProjectStatus s : ProjectStatus.values()) {
            status.addItem(new Option<>(s, s.label()));
        }
        selectOption(status, editing ? project.status() : ProjectStatus.PLANNING);
        status.setEnabled(editing);
        JComboBox<Option<Long>> manager = new JComboBox<>();
        for (EmployeeView m : managers) {
            manager.addItem(new Option<>(m.id(), m.fullName() + " (" + m.role().label() + ")"));
        }
        if (editing && project.managerId() != null) {
            selectOption(manager, project.managerId());
        } else if (currentUser != null) {
            selectOption(manager, currentUser.id());
        }

        List<String> labels = new ArrayList<>(List.of("Tên dự án *", "Mô tả", "Ngày bắt đầu (yyyy-MM-dd)",
                "Deadline (yyyy-MM-dd)", "Trạng thái"));
        List<Component> fields = new ArrayList<>(List.of(name, new JScrollPane(description), start, deadline, status));
        if (isAdmin()) {
            labels.add("Quản lý phụ trách");
            fields.add(manager);
        }
        JPanel panel = formPanel(labels.toArray(new String[0]), fields.toArray(new Component[0]));
        if (!editing) {
            panel.add(noteLabel("Dự án mới ở trạng thái \"Lên kế hoạch\"."), fullRow(labels.size()));
        }
        if (!confirm(panel, editing ? "Sửa dự án #" + project.id() : "Tạo dự án mới")) {
            return;
        }
        LocalDate startDate;
        LocalDate deadlineDate;
        try {
            startDate = parseDate(start.getText());
            deadlineDate = parseDate(deadline.getText());
        } catch (DateTimeParseException ex) {
            error("Ngày không hợp lệ, hãy nhập theo dạng yyyy-MM-dd.");
            return;
        }
        Map<String, Serializable> data = map(
                "name", name.getText(),
                "description", description.getText(),
                "startDate", startDate,
                "deadline", deadlineDate,
                "managerId", isAdmin() ? selected(manager) : null);
        if (!editing) {
            submit(Action.CREATE_PROJECT, data, this::refreshProjectViews);
            return;
        }
        ProjectStatus newStatus = selected(status);
        data.put("projectId", project.id());
        data.put("status", newStatus);
        boolean confirmIncomplete = false;
        if (newStatus == ProjectStatus.COMPLETED && project.status() != ProjectStatus.COMPLETED && project.openTasks() > 0) {
            confirmIncomplete = JOptionPane.showConfirmDialog(this, "Dự án còn " + project.openTasks()
                            + " công việc chưa hoàn thành.\nVẫn đánh dấu dự án đã hoàn thành? "
                            + "(Công việc sẽ chỉ được xem, không cập nhật được nữa)", "Xác nhận hoàn thành dự án",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
            if (!confirmIncomplete) {
                return;
            }
        }
        data.put("confirmIncomplete", confirmIncomplete);
        submit(Action.UPDATE_PROJECT, data, this::refreshProjectViews);
    }

    private void refreshProjectViews() {
        refreshProjects();
        refreshTasks();
        refreshDashboard();
    }

    private void deleteProject(ProjectView project) {
        String message = "Xóa dự án \"" + project.name() + "\"?\n\n"
                + "• " + project.totalTasks() + " công việc của dự án sẽ bị xóa theo\n"
                + "• " + project.members().size() + " thành viên sẽ được gỡ khỏi dự án\n\n"
                + "Thao tác này không thể hoàn tác.";
        confirmThen(message, "Xóa dự án", () -> submit(Action.DELETE_PROJECT, map("projectId", project.id()),
                this::refreshProjectViews));
    }

    /** Hộp thoại quản lý thành viên: thêm / gỡ nhân viên khỏi dự án. */
    private void membersDialog(ProjectView project) {
        call(Action.LIST_ASSIGNABLE_EMPLOYEES, response -> {
            List<EmployeeView> assignable = castList(response.getData());
            JDialog dialog = new JDialog(this, "Thành viên dự án: " + project.name(), true);
            JPanel root = new JPanel(new BorderLayout(0, 12));
            root.setBorder(new EmptyBorder(16, 16, 16, 16));
            JTable memberTable = table();
            JComboBox<Option<Long>> candidates = new JComboBox<>();
            JButton add = button("Thêm vào dự án", GREEN);
            JButton remove = button("Gỡ thành viên đã chọn", RED);
            JButton close = button("Đóng", LIGHT_BUTTON);
            ProjectView[] current = {project};

            Runnable render = () -> {
                DefaultTableModel model = model("ID", "Họ tên", "Username", "Trạng thái", "Email");
                for (EmployeeView m : current[0].members()) {
                    model.addRow(new Object[]{m.id(), m.fullName(), m.username(), m.status().label(), nullSafe(m.email())});
                }
                memberTable.setModel(model);
                candidates.removeAllItems();
                for (EmployeeView e : assignable) {
                    boolean member = current[0].members().stream().anyMatch(m -> m.id().equals(e.id()));
                    if (!member) {
                        candidates.addItem(new Option<>(e.id(), e.fullName() + " (@" + e.username() + ")"));
                    }
                }
                add.setEnabled(candidates.getItemCount() > 0);
            };
            render.run();

            add.addActionListener(e -> call(Action.ADD_PROJECT_MEMBER,
                    map("projectId", project.id(), "employeeId", selected(candidates)), r -> {
                        current[0] = (ProjectView) r.getData();
                        render.run();
                        refreshProjects();
                    }));
            remove.addActionListener(e -> {
                Long memberId = selectedId(memberTable);
                if (memberId == null) {
                    info("Hãy chọn thành viên cần gỡ.");
                    return;
                }
                call(Action.REMOVE_PROJECT_MEMBER, map("projectId", project.id(), "employeeId", memberId), r -> {
                    current[0] = (ProjectView) r.getData();
                    render.run();
                    refreshProjects();
                });
            });
            close.addActionListener(e -> dialog.dispose());

            JPanel addRow = transparentPanel(new BorderLayout(8, 0));
            addRow.add(candidates, BorderLayout.CENTER);
            addRow.add(add, BorderLayout.EAST);
            JPanel bottom = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            bottom.add(remove);
            bottom.add(close);
            root.add(addRow, BorderLayout.NORTH);
            root.add(styledScroll(memberTable), BorderLayout.CENTER);
            JPanel south = transparentPanel(new BorderLayout(0, 8));
            south.add(noteLabel("Chỉ gỡ được thành viên không còn công việc chưa hoàn thành trong dự án."), BorderLayout.NORTH);
            south.add(bottom, BorderLayout.SOUTH);
            root.add(south, BorderLayout.SOUTH);
            dialog.setContentPane(root);
            dialog.setSize(720, 460);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
    }

    private void showProjectDetail(ProjectView p) {
        String members = p.members().isEmpty() ? "(chưa có)" : p.members().stream()
                .map(m -> "  - " + m.fullName() + " (@" + m.username() + ")" + (m.status() == AccountStatus.ACTIVE
                        ? "" : " [" + m.status().label() + "]"))
                .reduce((a, b) -> a + "\n" + b).orElse("");
        JTextArea text = new JTextArea("""
                Tên dự án        : %s
                Quản lý phụ trách: %s
                Thời gian        : %s → %s
                Trạng thái       : %s
                Công việc        : %d hoàn thành / %d (còn %d chưa xong) - %d%%
                Mô tả            : %s

                Thành viên (%d):
                %s
                """.formatted(p.name(), nullSafe(p.managerName()), display(p.startDate()), display(p.deadline()),
                p.status().label(), p.completedTasks(), p.totalTasks(), p.openTasks(), p.progress(),
                nullSafe(p.description()), p.members().size(), members));
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(620, 340));
        JOptionPane.showMessageDialog(this, scroll, "Chi tiết dự án", JOptionPane.PLAIN_MESSAGE);
    }

    // ================================================================= công việc

    private JPanel taskPanel() {
        taskTable = table();
        JPanel actions = toolbarPanel();
        taskProjectFilter = new JComboBox<>();
        taskProjectFilter.addItem(new Option<>(null, "Tất cả dự án"));
        taskProjectFilter.addActionListener(e -> {
            if (!updatingTaskFilter) {
                renderTasks();
            }
        });
        actions.add(new JLabel("Dự án:"));
        actions.add(taskProjectFilter);
        JButton refresh = button("Tải lại", LIGHT_BUTTON);
        refresh.addActionListener(e -> refreshTasks());
        actions.add(refresh);
        JButton detail = button("Chi tiết", BLUE);
        detail.addActionListener(e -> withSelectedTask(this::showTaskDetail));
        actions.add(detail);

        if (isStaff()) {
            JButton create = button("+ Giao việc", GREEN);
            JButton edit = button("Sửa", NAVY);
            JButton transfer = button("Chuyển giao", BLUE_DARK);
            JButton reopen = button("Mở lại", ORANGE);
            JButton delete = button("Xóa", RED);
            create.addActionListener(e -> createTaskDialog());
            edit.addActionListener(e -> withSelectedTask(this::editTaskDialog));
            transfer.addActionListener(e -> withSelectedTask(this::transferTaskDialog));
            reopen.addActionListener(e -> withSelectedTask(task -> confirmThen("Mở lại công việc \"" + task.title()
                    + "\"?\nCông việc chuyển về trạng thái đang thực hiện.", "Mở lại",
                    () -> submit(Action.REOPEN_TASK, map("taskId", task.id()), this::refreshTaskViews))));
            delete.addActionListener(e -> withSelectedTask(task -> confirmThen("Xóa công việc \"" + task.title() + "\"?",
                    "Xóa", () -> submit(Action.DELETE_TASK, map("taskId", task.id()), this::refreshTaskViews))));
            actions.add(create);
            actions.add(edit);
            actions.add(transfer);
            actions.add(reopen);
            actions.add(delete);
        } else {
            JButton progress = button("Cập nhật tiến độ", BLUE_DARK);
            JButton done = button("Check hoàn thành", GREEN);
            JButton note = button("Ghi chú", NAVY);
            JButton submitResult = button("Nộp kết quả", ORANGE);
            progress.addActionListener(e -> withSelectedTask(this::progressDialog));
            done.addActionListener(e -> withSelectedTask(task -> confirmThen("Đánh dấu hoàn thành \"" + task.title() + "\"?",
                    "Hoàn thành", () -> submit(Action.COMPLETE_TASK, map("taskId", task.id()), this::refreshTaskViews))));
            note.addActionListener(e -> withSelectedTask(this::noteDialog));
            submitResult.addActionListener(e -> withSelectedTask(this::submitResultDialog));
            actions.add(progress);
            actions.add(done);
            actions.add(note);
            actions.add(submitResult);
        }
        return pageShell(isStaff() ? "Quản lý công việc" : "Công việc của tôi",
                isStaff() ? "Giao việc cho thành viên dự án, theo dõi tiến độ, chuyển giao khi cần."
                        : "Xem deadline, cập nhật tiến độ, ghi chú và check hoàn thành.",
                actions, styledScroll(taskTable));
    }

    private void refreshTasks() {
        call(isStaff() ? Action.LIST_TASKS : Action.MY_TASKS, response -> {
            tasks.clear();
            tasks.addAll(castList(response.getData()));
            renderTasks();
        });
    }

    private void refreshTaskViews() {
        refreshTasks();
        refreshProjects();
        refreshDashboard();
    }

    private void updateTaskProjectFilter() {
        if (taskProjectFilter == null) {
            return;
        }
        Long selectedProject = selected(taskProjectFilter);
        updatingTaskFilter = true;
        taskProjectFilter.removeAllItems();
        taskProjectFilter.addItem(new Option<>(null, "Tất cả dự án"));
        for (ProjectView p : projects) {
            taskProjectFilter.addItem(new Option<>(p.id(), p.name()));
        }
        selectOption(taskProjectFilter, selectedProject);
        updatingTaskFilter = false;
        renderTasks();
    }

    private void renderTasks() {
        if (taskTable == null) {
            return;
        }
        Long projectFilter = selected(taskProjectFilter);
        DefaultTableModel model = isStaff()
                ? model("ID", "Công việc", "Dự án", "Nhân viên", "Deadline", "Ưu tiên", "Tiến độ", "Trạng thái", "Ghi chú")
                : model("ID", "Công việc", "Dự án", "Người giao", "Deadline", "Ưu tiên", "Tiến độ", "Trạng thái", "Ghi chú");
        for (TaskView t : tasks) {
            if (projectFilter != null && !projectFilter.equals(t.projectId())) {
                continue;
            }
            String status = t.status().label() + (t.overdue() ? " • Quá hạn" : "")
                    + (t.projectClosed() ? " • DA đã đóng" : "");
            model.addRow(new Object[]{t.id(), t.title(), t.projectName(),
                    isStaff() ? t.assigneeName() : t.assignerName(), display(t.deadline()), t.priority().label(),
                    t.progress() + "%", status, nullToEmpty(t.note())});
        }
        taskTable.setModel(model);
        setColumnWidths(taskTable, 40, 220, 170, 130, 92, 80, 62, 160, 170);
    }

    private void withSelectedTask(Consumer<TaskView> action) {
        Long id = selectedId(taskTable);
        if (id == null) {
            info("Hãy chọn một công việc trong bảng.");
            return;
        }
        tasks.stream().filter(t -> t.id().equals(id)).findFirst().ifPresent(action);
    }

    private void createTaskDialog() {
        List<ProjectView> openProjects = projects.stream().filter(p -> !p.status().isClosed()).toList();
        if (openProjects.isEmpty()) {
            info("Chưa có dự án nào đang mở để giao việc. Hãy tạo dự án trước.");
            return;
        }
        JComboBox<Option<Long>> project = new JComboBox<>();
        for (ProjectView p : openProjects) {
            project.addItem(new Option<>(p.id(), p.name()));
        }
        Long filter = selected(taskProjectFilter);
        if (filter != null) {
            selectOption(project, filter);
        }
        JComboBox<Option<Long>> assignee = new JComboBox<>();
        JLabel memberHint = noteLabel(" ");
        Runnable loadMembers = () -> {
            assignee.removeAllItems();
            Long projectId = selected(project);
            openProjects.stream().filter(p -> p.id().equals(projectId)).findFirst().ifPresent(p -> {
                p.members().stream().filter(m -> m.status() == AccountStatus.ACTIVE)
                        .forEach(m -> assignee.addItem(new Option<>(m.id(), m.fullName() + " (@" + m.username() + ")")));
                memberHint.setText(assignee.getItemCount() == 0
                        ? "Dự án chưa có thành viên đang hoạt động. Hãy thêm thành viên ở mục Dự án."
                        : "Chỉ giao việc cho thành viên đang hoạt động của dự án.");
            });
        };
        project.addActionListener(e -> loadMembers.run());
        loadMembers.run();
        JTextField title = new JTextField();
        JTextArea description = new JTextArea(4, 26);
        JTextField start = new JTextField(LocalDate.now().toString());
        JTextField deadline = new JTextField(LocalDate.now().plusDays(7).toString());
        JComboBox<Option<TaskPriority>> priority = priorityCombo(TaskPriority.MEDIUM);
        JPanel panel = formPanel(new String[]{"Dự án *", "Giao cho *", "Tên công việc *", "Mô tả",
                        "Ngày bắt đầu (yyyy-MM-dd)", "Deadline (yyyy-MM-dd)", "Mức độ ưu tiên"},
                new Component[]{project, assignee, title, new JScrollPane(description), start, deadline, priority});
        panel.add(memberHint, fullRow(7));
        if (!confirm(panel, "Giao công việc")) {
            return;
        }
        if (selected(assignee) == null) {
            error("Hãy chọn nhân viên thực hiện.");
            return;
        }
        try {
            submit(Action.CREATE_TASK, map(
                    "projectId", selected(project),
                    "assigneeId", selected(assignee),
                    "title", title.getText(),
                    "description", description.getText(),
                    "startDate", parseDate(start.getText()),
                    "deadline", parseDate(deadline.getText()),
                    "priority", selected(priority)), this::refreshTaskViews);
        } catch (DateTimeParseException ex) {
            error("Ngày không hợp lệ, hãy nhập theo dạng yyyy-MM-dd.");
        }
    }

    private void editTaskDialog(TaskView task) {
        JTextField title = new JTextField(task.title());
        JTextArea description = new JTextArea(nullToEmpty(task.description()), 4, 26);
        JTextField start = new JTextField(dateText(task.startDate()));
        JTextField deadline = new JTextField(dateText(task.deadline()));
        JComboBox<Option<TaskPriority>> priority = priorityCombo(task.priority());
        JPanel panel = formPanel(new String[]{"Tên công việc *", "Mô tả", "Ngày bắt đầu (yyyy-MM-dd)",
                        "Deadline (yyyy-MM-dd)", "Mức độ ưu tiên"},
                new Component[]{title, new JScrollPane(description), start, deadline, priority});
        if (!confirm(panel, "Sửa công việc #" + task.id())) {
            return;
        }
        try {
            submit(Action.UPDATE_TASK, map(
                    "taskId", task.id(),
                    "title", title.getText(),
                    "description", description.getText(),
                    "startDate", parseDate(start.getText()),
                    "deadline", parseDate(deadline.getText()),
                    "priority", selected(priority)), this::refreshTaskViews);
        } catch (DateTimeParseException ex) {
            error("Ngày không hợp lệ, hãy nhập theo dạng yyyy-MM-dd.");
        }
    }

    private void transferTaskDialog(TaskView task) {
        call(Action.LIST_ASSIGNABLE_EMPLOYEES, response -> {
            List<EmployeeView> receivers = this.<EmployeeView>castList(response.getData()).stream()
                    .filter(e -> !e.id().equals(task.assigneeId())).toList();
            if (receivers.isEmpty()) {
                error("Không có nhân viên nào khác đang hoạt động.");
                return;
            }
            JComboBox<Option<Long>> receiver = employeeCombo(receivers);
            JPanel panel = formPanel(new String[]{"Đang giao cho", "Chuyển cho"},
                    new Component[]{new JLabel(task.assigneeName()), receiver});
            panel.add(noteLabel("Người nhận sẽ được thêm vào dự án nếu chưa là thành viên."), fullRow(2));
            if (confirm(panel, "Chuyển giao: " + task.title())) {
                submit(Action.TRANSFER_TASK, map("taskId", task.id(), "assigneeId", selected(receiver)), this::refreshTaskViews);
            }
        });
    }

    private void progressDialog(TaskView task) {
        JTextField progress = new JTextField(String.valueOf(task.progress()));
        JPanel panel = formPanel(new String[]{"Tiến độ (0 - 100)"}, new Component[]{progress});
        panel.add(noteLabel("Nhập 100 để đánh dấu hoàn thành."), fullRow(1));
        if (confirm(panel, "Cập nhật tiến độ: " + task.title())) {
            int value;
            try {
                value = Integer.parseInt(progress.getText().trim());
            } catch (NumberFormatException ex) {
                error("Tiến độ phải là số nguyên từ 0 đến 100.");
                return;
            }
            submit(Action.UPDATE_TASK_PROGRESS, map("taskId", task.id(), "progress", value), this::refreshTaskViews);
        }
    }

    private void noteDialog(TaskView task) {
        JTextArea note = new JTextArea(nullToEmpty(task.note()), 5, 28);
        note.setLineWrap(true);
        if (confirm(new JScrollPane(note), "Ghi chú: " + task.title())) {
            submit(Action.ADD_TASK_NOTE, map("taskId", task.id(), "note", note.getText()), this::refreshTasks);
        }
    }

    private void submitResultDialog(TaskView task) {
        JTextArea result = new JTextArea(nullToEmpty(task.resultText()), 5, 28);
        JTextField link = new JTextField(nullToEmpty(task.resultLink()));
        JTextArea note = new JTextArea(3, 28);
        JPanel panel = formPanel(new String[]{"Nội dung kết quả *", "Link kết quả", "Ghi chú"},
                new Component[]{new JScrollPane(result), link, new JScrollPane(note)});
        panel.add(noteLabel("Nộp kết quả sẽ đánh dấu công việc hoàn thành (100%)."), fullRow(3));
        if (confirm(panel, "Nộp kết quả: " + task.title())) {
            submit(Action.SUBMIT_TASK_RESULT, map("taskId", task.id(), "resultText", result.getText(),
                    "resultLink", link.getText(), "note", note.getText()), this::refreshTaskViews);
        }
    }

    private void showTaskDetail(TaskView t) {
        JTextArea text = new JTextArea("""
                Công việc   : %s
                Dự án       : %s%s
                Người làm   : %s
                Người giao  : %s
                Thời gian   : %s → %s%s
                Ưu tiên     : %s
                Trạng thái  : %s - %d%%
                Mô tả       : %s
                Ghi chú     : %s
                Kết quả     : %s
                Link        : %s
                """.formatted(t.title(), t.projectName(), t.projectClosed() ? " (đã đóng)" : "",
                t.assigneeName(), nullSafe(t.assignerName()), display(t.startDate()), display(t.deadline()),
                t.overdue() ? "  (QUÁ HẠN)" : "", t.priority().label(), t.status().label(), t.progress(),
                nullSafe(t.description()), nullSafe(t.note()), nullSafe(t.resultText()), nullSafe(t.resultLink())));
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(620, 320));
        JOptionPane.showMessageDialog(this, scroll, "Chi tiết công việc #" + t.id(), JOptionPane.PLAIN_MESSAGE);
    }

    // ================================================================= hồ sơ

    private JPanel profilePanel() {
        profileInfo = new JTextArea();
        profileInfo.setEditable(false);
        profileInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        profileInfo.setBorder(new EmptyBorder(18, 18, 18, 18));
        profileInfo.setText(profileText(currentUser));
        JPanel actions = toolbarPanel();
        JButton update = button("Cập nhật thông tin", GREEN);
        JButton password = button("Đổi mật khẩu", NAVY);
        update.addActionListener(e -> updateProfileDialog());
        password.addActionListener(e -> changePasswordDialog());
        actions.add(update);
        actions.add(password);
        return pageShell("Tài khoản cá nhân", "Xem và cập nhật thông tin của bạn, đổi mật khẩu.",
                actions, styledScroll(profileInfo));
    }

    private void refreshProfile() {
        call(Action.ME, response -> {
            currentUser = (EmployeeView) response.getData();
            profileInfo.setText(profileText(currentUser));
            updateUserLabel();
        });
    }

    private void updateProfileDialog() {
        JTextField fullName = new JTextField(currentUser.fullName());
        JTextField email = new JTextField(nullToEmpty(currentUser.email()));
        JTextField phone = new JTextField(nullToEmpty(currentUser.phone()));
        JPanel panel = formPanel(new String[]{"Họ tên", "Email", "Số điện thoại"}, new Component[]{fullName, email, phone});
        if (confirm(panel, "Cập nhật thông tin")) {
            call(Action.UPDATE_PROFILE, map("fullName", fullName.getText(), "email", email.getText(),
                    "phone", phone.getText()), response -> {
                currentUser = (EmployeeView) response.getData();
                profileInfo.setText(profileText(currentUser));
                updateUserLabel();
                info(response.getMessage());
            });
        }
    }

    private void changePasswordDialog() {
        JPasswordField oldPassword = new JPasswordField();
        JPasswordField newPassword = new JPasswordField();
        JPasswordField confirmPassword = new JPasswordField();
        JPanel panel = formPanel(new String[]{"Mật khẩu cũ", "Mật khẩu mới (≥ 6 ký tự)", "Nhập lại mật khẩu mới"},
                new Component[]{oldPassword, newPassword, confirmPassword});
        if (confirm(panel, "Đổi mật khẩu")) {
            if (!new String(newPassword.getPassword()).equals(new String(confirmPassword.getPassword()))) {
                error("Mật khẩu nhập lại không khớp.");
                return;
            }
            submit(Action.CHANGE_PASSWORD, map("oldPassword", new String(oldPassword.getPassword()),
                    "newPassword", new String(newPassword.getPassword())), null);
        }
    }

    private static String profileText(EmployeeView e) {
        return """
                Mã tài khoản  : %s
                Tên đăng nhập : %s
                Họ tên        : %s
                Email         : %s
                Số điện thoại : %s
                Vai trò       : %s
                Trạng thái    : %s
                """.formatted(e.id(), e.username(), e.fullName(), nullSafe(e.email()), nullSafe(e.phone()),
                e.role().label(), e.status().label());
    }

    // ================================================================= thành phần giao diện dùng chung

    private <T> JComboBox<Option<T>> comboOf(List<Option<T>> options) {
        JComboBox<Option<T>> combo = new JComboBox<>();
        options.forEach(combo::addItem);
        return combo;
    }

    private JComboBox<Option<Long>> employeeCombo(List<EmployeeView> employees) {
        return comboOf(employees.stream()
                .map(e -> new Option<>(e.id(), e.fullName() + " (@" + e.username() + ")")).toList());
    }

    private JComboBox<Option<TaskPriority>> priorityCombo(TaskPriority selectedPriority) {
        JComboBox<Option<TaskPriority>> combo = new JComboBox<>();
        for (TaskPriority p : TaskPriority.values()) {
            combo.addItem(new Option<>(p, p.label()));
        }
        selectOption(combo, selectedPriority == null ? TaskPriority.MEDIUM : selectedPriority);
        return combo;
    }

    private static <T> T selected(JComboBox<Option<T>> combo) {
        if (combo == null) {
            return null;
        }
        Object item = combo.getSelectedItem();
        if (item instanceof Option<?> option) {
            @SuppressWarnings("unchecked")
            T value = (T) option.value();
            return value;
        }
        return null;
    }

    private static <T> void selectOption(JComboBox<Option<T>> combo, T value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (Objects.equals(combo.getItemAt(i).value(), value)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static void menuItem(JPopupMenu menu, String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    /** Nội dung hộp thoại dạng HTML có độ rộng cố định, giữ xuống dòng. */
    private static JLabel messageLabel(String message) {
        JLabel label = new JLabel("<html><div style='width:380px'>" + escape(message).replace("\n", "<br>")
                + "</div></html>");
        label.setFont(BODY_FONT);
        return label;
    }

    private void confirmThen(String message, String buttonText, Runnable action) {
        int choice = JOptionPane.showOptionDialog(this, messageLabel(message), "Xác nhận", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Hủy", buttonText}, "Hủy");
        if (choice == 1) {
            action.run();
        }
    }

    private JPanel identityBlock(EmployeeView employee) {
        JPanel panel = transparentPanel(new BorderLayout(10, 0));
        panel.add(new AvatarLabel(initials(employee.fullName()), roleColor(employee.role())), BorderLayout.WEST);
        panel.add(twoLines(employee.fullName(), "@" + employee.username(), NAVY), BorderLayout.CENTER);
        return panel;
    }

    private static JPanel twoLines(String first, String second, Color firstColor) {
        JPanel panel = transparentPanel(new BorderLayout(0, 2));
        JLabel top = new JLabel(first);
        top.setToolTipText(first);
        top.setFont(new Font("Segoe UI", Font.BOLD, 13));
        top.setForeground(firstColor);
        JLabel bottom = new JLabel(second);
        bottom.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottom.setForeground(MUTED);
        panel.add(top, BorderLayout.NORTH);
        panel.add(bottom, BorderLayout.CENTER);
        return panel;
    }

    /** Mỗi ô có độ rộng cố định theo tỷ lệ để các dòng thẳng cột với nhau. */
    private static void addCell(JPanel panel, int col, Component component, double weight) {
        JPanel cell = transparentPanel(new BorderLayout());
        int width = (int) Math.round(weight * 680);
        if (component instanceof JButton || col == 5) {
            width = Math.max(width, component.getPreferredSize().width);
        }
        cell.setPreferredSize(new Dimension(width, component.getPreferredSize().height));
        cell.setMinimumSize(new Dimension(width, component.getPreferredSize().height));
        cell.add(component, component instanceof JLabel || component instanceof JButton
                ? BorderLayout.WEST : BorderLayout.CENTER);
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = col;
        rule.gridy = 0;
        rule.weightx = weight;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.insets = new Insets(0, 0, 0, col == 5 ? 0 : 12);
        panel.add(cell, rule);
    }

    private static JPanel miniStat(String label, long value, Color color) {
        JPanel panel = new RoundedPanel(new BorderLayout(0, 2), tint(color),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 70), 16);
        panel.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel valueView = new JLabel(String.valueOf(value));
        valueView.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueView.setForeground(color.darker());
        JLabel labelView = new JLabel(label);
        labelView.setFont(new Font("Segoe UI", Font.BOLD, 11));
        labelView.setForeground(new Color(71, 85, 105));
        panel.add(valueView, BorderLayout.NORTH);
        panel.add(labelView, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel counterPill(String label, long value, Color color) {
        return miniStat(label, value, color);
    }

    private static JPanel statCard(String label, String value, String note, Color accent) {
        JPanel card = new RoundedPanel(new BorderLayout(0, 6), tint(accent),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80), 22);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel labelView = new JLabel(label);
        labelView.setFont(LABEL_FONT);
        labelView.setForeground(accent.darker());
        JLabel valueView = new JLabel(value);
        valueView.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueView.setForeground(NAVY);
        JLabel noteView = new JLabel(note);
        noteView.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteView.setForeground(new Color(71, 85, 105));
        card.add(labelView, BorderLayout.NORTH);
        card.add(valueView, BorderLayout.CENTER);
        card.add(noteView, BorderLayout.SOUTH);
        return card;
    }

    private static JProgressBar progressBar(int value) {
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(value);
        progress.setPreferredSize(new Dimension(0, 10));
        progress.setForeground(GREEN);
        progress.setBackground(new Color(226, 232, 240));
        return progress;
    }

    private static JLabel sectionTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(NAVY);
        return title;
    }

    /** Nhãn ghi chú tự xuống dòng (giới hạn độ rộng để không bị cắt chữ trong hộp thoại). */
    private static JLabel noteLabel(String text) {
        String body = text.startsWith("<html>") ? text.substring(6).replace("</html>", "") : escape(text);
        JLabel label = new JLabel("<html><div style='width:430px'>" + body + "</div></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(MUTED);
        return label;
    }

    private static JLabel badge(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setForeground(color.darker());
        label.setBackground(tint(color));
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return label;
    }

    private static Color priorityColor(TaskPriority priority) {
        return switch (priority) {
            case LOW -> BLUE;
            case MEDIUM -> ORANGE;
            case HIGH, URGENT -> RED;
        };
    }

    private static Color taskStatusColor(TaskStatus status) {
        return switch (status) {
            case TODO -> BLUE;
            case IN_PROGRESS -> ORANGE;
            case ON_HOLD -> SLATE;
            case COMPLETED -> GREEN;
            case CANCELLED -> RED;
        };
    }

    private static Color projectStatusColor(ProjectStatus status) {
        return switch (status) {
            case PLANNING -> BLUE;
            case IN_PROGRESS -> ORANGE;
            case COMPLETED -> GREEN;
            case CANCELLED -> RED;
        };
    }

    private static Color accountStatusColor(AccountStatus status) {
        return switch (status) {
            case PENDING -> ORANGE;
            case ACTIVE -> GREEN;
            case ON_LEAVE -> new Color(8, 145, 178);
            case LOCKED -> RED;
            case RESIGNED -> new Color(71, 85, 105);
        };
    }

    private static Color roleColor(Role role) {
        return switch (role) {
            case ADMIN -> RED;
            case MANAGER -> PURPLE;
            case EMPLOYEE -> BLUE;
        };
    }

    private JPanel pageShell(String title, String subtitle, JPanel actions, Component body) {
        JPanel panel = contentPanel();
        JPanel top = transparentPanel(new BorderLayout(0, 14));
        top.add(pageHeader(title, subtitle), BorderLayout.NORTH);
        top.add(actions, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel pageHeader(String title, String subtitle) {
        JPanel header = new HeaderPanel();
        header.setLayout(new BorderLayout(0, 6));
        header.setBorder(new EmptyBorder(22, 26, 22, 26));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(219, 234, 254));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.CENTER);
        return header;
    }

    private static JPanel brandPanel() {
        JPanel panel = transparentPanel(new BorderLayout(12, 0));
        panel.setPreferredSize(new Dimension(234, 54));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel logo = new JLabel("", JLabel.CENTER);
        ImageIcon icon = logoIcon(46);
        if (icon != null) {
            logo.setIcon(icon);
        } else {
            logo.setText("TF");
            logo.setForeground(Color.WHITE);
            logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        }
        logo.setPreferredSize(new Dimension(48, 48));
        JPanel text = transparentPanel(new BorderLayout(0, 2));
        JLabel brand = new JLabel("TASKFLOW");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brand.setForeground(Color.WHITE);
        JLabel caption = new JLabel("Quản lý phân công công việc");
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        caption.setForeground(new Color(148, 163, 184));
        text.add(brand, BorderLayout.NORTH);
        text.add(caption, BorderLayout.CENTER);
        panel.add(logo, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    private static ImageIcon logoIcon(int size) {
        java.net.URL url = TaskSwingClient.class.getResource("/assets/logo.png");
        if (url == null) {
            return null;
        }
        try {
            BufferedImage source = ImageIO.read(url);
            BufferedImage cropped = cropTransparentOrWhite(source);
            return new ImageIcon(cropped.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            return new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
        }
    }

    private static BufferedImage cropTransparentOrWhite(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = 0;
        int maxY = 0;
        boolean found = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                int red = (argb >>> 16) & 0xff;
                int green = (argb >>> 8) & 0xff;
                int blue = argb & 0xff;
                if (alpha > 20 && (red < 246 || green < 246 || blue < 246)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    found = true;
                }
            }
        }
        return found ? image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1) : image;
    }

    private static JPanel loginIntroPanel() {
        JPanel panel = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        JLabel appName = new JLabel("TASKFLOW");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 42));
        appName.setForeground(Color.WHITE);
        panel.add(appName, gbc);
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 0, 0, 0);
        JLabel title = new JLabel("Quản lý phân công công việc");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(226, 232, 240));
        panel.add(title, gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(18, 0, 0, 0);
        JTextArea copy = new JTextArea("Admin quản trị toàn hệ thống, quản lý điều phối dự án, nhân viên cập nhật "
                + "tiến độ công việc - trên mô hình client/server.");
        copy.setEditable(false);
        copy.setFocusable(false);
        copy.setLineWrap(true);
        copy.setWrapStyleWord(true);
        copy.setOpaque(false);
        copy.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        copy.setForeground(new Color(219, 234, 254));
        panel.add(copy, gbc);
        gbc.gridy = 3;
        gbc.insets = new Insets(26, 0, 0, 0);
        JPanel chips = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.add(chip("Java Swing"));
        chips.add(chip("Socket TCP"));
        chips.add(chip("JPA + Hibernate"));
        panel.add(chips, gbc);
        return panel;
    }

    private static JLabel chip(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setForeground(Color.WHITE);
        label.setBackground(new Color(255, 255, 255, 42));
        label.setBorder(new EmptyBorder(8, 12, 8, 12));
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    private static JPanel contentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        return panel;
    }

    private static JPanel toolbarPanel() {
        JPanel panel = cardPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), 10);
        panel.setBorder(new EmptyBorder(6, 10, 6, 10));
        return panel;
    }

    private static JPanel cardPanel(java.awt.LayoutManager layout, int padding) {
        JPanel panel = new RoundedPanel(layout, CARD, BORDER, 22);
        panel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    private static JPanel transparentPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JScrollPane styledScroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(new LineBorder(BORDER, 1));
        scroll.getViewport().setBackground(CARD);
        return scroll;
    }

    private static JButton button(String text, Color color) {
        JButton button = new ModernButton(text);
        styleButton(button, color);
        return button;
    }

    private static void styleButton(JButton button, Color background) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(background);
        button.setForeground(isLight(background) ? NAVY : Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(9, 14, 9, 14));
        if (button instanceof ModernButton modernButton) {
            modernButton.setButtonColor(background);
        }
    }

    private static boolean isLight(Color color) {
        return (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000 > 170;
    }

    private static void styleInput(JTextField field) {
        field.setFont(BODY_FONT);
        field.setForeground(NAVY);
        field.setBackground(Color.WHITE);
        field.setCaretColor(BLUE);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1), new EmptyBorder(8, 10, 8, 10)));
    }

    private static JTable table() {
        JTable table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(34);
        table.setFont(BODY_FONT);
        table.setGridColor(new Color(241, 245, 249));
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(NAVY);
        table.getTableHeader().setFont(LABEL_FONT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setDefaultRenderer(Object.class, new ZebraRenderer());
        return table;
    }

    /** Cột hẹp (mã, ngày, %, ...) được giữ độ rộng tối thiểu để không bị cắt chữ. */
    private static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            if (widths[i] <= 100) {
                table.getColumnModel().getColumn(i).setMinWidth(widths[i]);
            }
        }
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static Long selectedId(JTable table) {
        if (table == null || table.getSelectedRow() < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object value = table.getModel().getValueAt(modelRow, 0);
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private static JPanel formPanel(String[] labels, Component[] fields) {
        JPanel panel = transparentPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(6, 4, 6, 4));
        for (int i = 0; i < labels.length; i++) {
            addField(panel, i, labels[i], fields[i]);
        }
        return panel;
    }

    private static GridBagConstraints fullRow(int row) {
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.gridy = row;
        rule.gridwidth = 2;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.insets = new Insets(6, 6, 0, 6);
        return rule;
    }

    private static void addField(JPanel panel, int row, String label, Component field) {
        if (field instanceof JTextField textField) {
            styleInput(textField);
        }
        if (field instanceof JComponent component && component.getPreferredSize().width < 300) {
            component.setPreferredSize(new Dimension(320, component.getPreferredSize().height));
        }
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_END;
        left.insets = new Insets(7, 6, 7, 12);
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(LABEL_FONT);
        labelComponent.setForeground(NAVY);
        panel.add(labelComponent, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(7, 0, 7, 6);
        panel.add(field, right);
    }

    private boolean confirm(Component panel, String title) {
        return JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, messageLabel(message), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, messageLabel(message), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1 ? parts[0].substring(0, 1).toUpperCase()
                : (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static Color tint(Color color) {
        return new Color(Math.min(255, color.getRed() + 210), Math.min(255, color.getGreen() + 205),
                Math.min(255, color.getBlue() + 190));
    }

    private static String display(LocalDate date) {
        return date == null ? "Chưa đặt" : date.toString();
    }

    private static String dateText(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private static String nullSafe(String text) {
        return text == null || text.isBlank() ? "Chưa có" : text;
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static LocalDate parseDate(String text) {
        return text == null || text.isBlank() ? null : LocalDate.parse(text.trim());
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object data) {
        return data == null ? List.of() : (List<T>) data;
    }

    private static Map<String, Serializable> map(Object... keyValues) {
        Map<String, Serializable> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                map.put((String) keyValues[i], (Serializable) keyValues[i + 1]);
            }
        }
        return map;
    }

    /** Một lựa chọn trong JComboBox: giá trị thật + nhãn hiển thị. */
    private record Option<T>(T value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** Panel trong JScrollPane luôn co giãn theo bề rộng vùng hiển thị (chỉ cuộn dọc). */
    private static class WidthTrackingPanel extends JPanel implements javax.swing.Scrollable {
        WidthTrackingPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            if (value != null) {
                setToolTipText(String.valueOf(value));
            }
            if (!isSelected) {
                component.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                String text = String.valueOf(value);
                component.setForeground(text.contains("Quá hạn") ? RED.darker() : new Color(30, 41, 59));
            }
            return component;
        }
    }

    private static class AvatarLabel extends JLabel {
        private final Color color;

        AvatarLabel(String text, Color color) {
            super(text, JLabel.CENTER);
            this.color = color;
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setPreferredSize(new Dimension(40, 40));
            setMinimumSize(new Dimension(40, 40));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int diameter = Math.min(getWidth(), getHeight()) - 2;
            g2.setColor(color);
            g2.fillOval((getWidth() - diameter) / 2, (getHeight() - diameter) / 2, diameter, diameter);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class ModernButton extends JButton {
        private Color buttonColor = BLUE;

        ModernButton(String text) {
            super(text);
        }

        void setButtonColor(Color buttonColor) {
            this.buttonColor = buttonColor;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = getModel().isPressed() ? buttonColor.darker() : buttonColor;
            if (getModel().isRollover()) {
                color = isLight(buttonColor) ? new Color(203, 213, 225) : buttonColor.brighter();
            }
            if (!isEnabled()) {
                color = new Color(203, 213, 225);
            }
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color border;
        private final int radius;

        RoundedPanel(java.awt.LayoutManager layout, Color fill, Color border, int radius) {
            super(layout);
            this.fill = fill;
            this.border = border;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class HeaderPanel extends JPanel {
        HeaderPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, BLUE_DARK, getWidth(), getHeight(), new Color(14, 165, 233)));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.setColor(new Color(255, 255, 255, 35));
            g2.fillOval(getWidth() - 180, -70, 240, 240);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, NAVY, getWidth(), getHeight(), BLUE_DARK));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillOval(-90, -90, 280, 280);
            g2.dispose();
        }
    }
}
