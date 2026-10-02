package com.company.project.client;

import com.company.project.dto.DashboardStats;
import com.company.project.dto.EmployeeDirectoryView;
import com.company.project.dto.EmployeeView;
import com.company.project.dto.ProjectView;
import com.company.project.dto.TaskView;
import com.company.project.entity.AccountStatus;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.network.Action;
import com.company.project.network.Request;
import com.company.project.network.Response;
import com.company.project.service.AppService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.imageio.ImageIO;
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
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskSwingClient extends JFrame {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String token;
    private EmployeeView currentUser;

    private CardLayout cardLayout;
    private JPanel mainCards;
    private JTable employeeTable;
    private JTable projectTable;
    private JTable taskTable;
    private JLabel statusLabel;
    private final List<JButton> navButtons = new ArrayList<>();
    private final List<EmployeeDirectoryView> employeeDirectory = new ArrayList<>();
    private JPanel employeeStatsPanel;
    private JPanel employeeRowsPanel;
    private JLabel employeeRangeLabel;
    private JTextField employeeSearchField;
    private JComboBox<String> employeeStatusFilter;
    private JComboBox<String> employeeRoleFilter;
    private int employeePage = 1;
    private static final int EMPLOYEE_PAGE_SIZE = 10;

    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color BLUE_DARK = new Color(29, 78, 216);
    private static final Color BLUE_SOFT = new Color(239, 246, 255);
    private static final Color GREEN = new Color(5, 150, 105);
    private static final Color ORANGE = new Color(245, 158, 11);
    private static final Color RED = new Color(225, 29, 72);
    private static final Color BACKGROUND = new Color(245, 247, 251);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color SIDEBAR = new Color(15, 23, 42);
    private static final Color SIDEBAR_ACTIVE = new Color(37, 99, 235);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                configureUi();
                TaskSwingClient client = new TaskSwingClient();
                client.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Lỗi khởi động", JOptionPane.ERROR_MESSAGE);
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
        UIManager.put("TabbedPane.font", BODY_FONT);
        UIManager.put("OptionPane.messageFont", BODY_FONT);
        UIManager.put("OptionPane.buttonFont", BODY_FONT);
    }

    public TaskSwingClient() throws Exception {
        connect("localhost", 2026);
        setTitle("Quản lý phân công công việc");
        setSize(1120, 720);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ImageIcon appLogo = logoIcon(48);
        if (appLogo != null) {
            setIconImage(appLogo.getImage());
        }
        showLoginScreen();
    }

    private void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
    }

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
        card.setPreferredSize(new Dimension(390, 440));
        JLabel title = new JLabel("Đăng nhập hệ thống");
        title.setFont(TITLE_FONT);
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Quản lý dự án, nhân viên và công việc");
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(new Color(100, 116, 139));
        JPanel heading = transparentPanel(new BorderLayout(0, 6));
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.CENTER);

        JPanel form = transparentPanel(new GridBagLayout());
        JTextField username = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        styleInput(username);
        styleInput(password);
        addField(form, 0, "Tên đăng nhập", username);
        addField(form, 1, "Mật khẩu", password);

        JPanel buttons = transparentPanel(new GridBagLayout());
        JButton loginButton = new ModernButton("Đăng nhập");
        JButton registerButton = new ModernButton("Đăng ký nhân viên mới");
        styleButton(loginButton, BLUE);
        styleButton(registerButton, NAVY);
        GridBagConstraints buttonRule = new GridBagConstraints();
        buttonRule.gridx = 0;
        buttonRule.fill = GridBagConstraints.HORIZONTAL;
        buttonRule.weightx = 1;
        buttonRule.insets = new Insets(0, 0, 10, 0);
        buttons.add(loginButton, buttonRule);
        buttonRule.gridy = 1;
        buttonRule.insets = new Insets(0, 0, 0, 0);
        buttons.add(registerButton, buttonRule);

        JLabel hint = new JLabel("Tài khoản quản lý mặc định: admin / admin123", JLabel.CENTER);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(new Color(100, 116, 139));

        card.add(heading, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(wrapSouth(buttons, hint), BorderLayout.SOUTH);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.fill = GridBagConstraints.NONE;
        hero.add(card, right);
        root.add(hero, BorderLayout.CENTER);

        loginButton.addActionListener(e -> {
            Response response = sendSafe(Action.LOGIN, null, map(
                    "username", username.getText(),
                    "password", new String(password.getPassword())
            ));
            if (response == null || !showResponse(response)) {
                return;
            }
            AppService.LoginResult result = (AppService.LoginResult) response.getData();
            token = result.token();
            currentUser = result.employee();
            showMainScreen();
        });
        registerButton.addActionListener(e -> showRegisterDialog());

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
        JPanel panel = formPanel(new String[]{"Tên đăng nhập", "Mật khẩu", "Họ tên", "Email", "Số điện thoại"},
                new java.awt.Component[]{username, password, fullName, email, phone});
        if (confirm(panel, "Đăng ký tài khoản")) {
            Response response = sendSafe(Action.REGISTER, null, map(
                    "username", username.getText(),
                    "password", new String(password.getPassword()),
                    "fullName", fullName.getText(),
                    "email", email.getText(),
                    "phone", phone.getText()
            ));
            showResponse(response);
        }
    }

    private void showMainScreen() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(286, 0));
        sidebar.setBorder(new EmptyBorder(24, 18, 18, 18));

        sidebar.add(brandPanel());
        sidebar.add(Box.createVerticalStrut(26));

        cardLayout = new CardLayout();
        mainCards = new JPanel(cardLayout);
        mainCards.setBackground(BACKGROUND);
        navButtons.clear();

        addNavigation(sidebar, "Tổng quan", "dashboard", dashboardPanel(), true);
        if (currentUser.role() == Role.MANAGER) {
            addNavigation(sidebar, "Nhân viên", "employees", employeePanel(), false);
            addNavigation(sidebar, "Thông tin dự án", "projects", projectPanel(true), false);
            addNavigation(sidebar, "Công việc", "tasks", taskPanel(true), false);
            addNavigation(sidebar, "Báo cáo", "reports", reportPanel(), false);
            refreshEmployees();
            refreshProjects();
            refreshAllTasks();
        } else {
            addNavigation(sidebar, "Công việc của tôi", "tasks", taskPanel(false), false);
            addNavigation(sidebar, "Thông tin dự án", "projects", projectPanel(false), false);
            refreshMyTasks();
            refreshProjects();
        }
        addNavigation(sidebar, "Hồ sơ", "profile", profilePanel(), false);

        sidebar.add(Box.createVerticalGlue());

        JPanel userCard = new RoundedPanel(new BorderLayout(10, 10), new Color(30, 41, 59), new Color(51, 65, 85), 18);
        userCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        userCard.setPreferredSize(new Dimension(250, 156));
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 156));
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel avatar = new AvatarLabel(initials(currentUser.fullName()));
        statusLabel = new JLabel("<html>" + currentUser.fullName() + "<br><span style='font-size:10px;color:#cbd5e1'>"
                + roleLabel(currentUser.role()) + "</span></html>");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(Color.WHITE);
        JPanel userInfo = transparentPanel(new BorderLayout(10, 0));
        userInfo.setPreferredSize(new Dimension(0, 48));
        userInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        userInfo.add(avatar, BorderLayout.WEST);
        userInfo.add(statusLabel, BorderLayout.CENTER);
        JPanel userActions = transparentPanel(new GridBagLayout());
        JButton profile = new ModernButton("Hồ sơ");
        JButton logout = new ModernButton("Đăng xuất");
        styleButton(profile, new Color(51, 65, 85));
        styleButton(logout, RED);
        GridBagConstraints actionRule = new GridBagConstraints();
        actionRule.gridx = 0;
        actionRule.gridy = 0;
        actionRule.weightx = 1;
        actionRule.fill = GridBagConstraints.HORIZONTAL;
        actionRule.insets = new Insets(0, 0, 0, 8);
        userActions.add(profile, actionRule);
        actionRule.gridx = 1;
        actionRule.insets = new Insets(0, 0, 0, 0);
        userActions.add(logout, actionRule);
        profile.addActionListener(e -> {
            cardLayout.show(mainCards, "profile");
            updateNavigation("profile");
        });
        logout.addActionListener(e -> logout());
        userCard.add(userInfo, BorderLayout.CENTER);
        userCard.add(userActions, BorderLayout.SOUTH);

        sidebar.add(userCard);

        root.add(sidebar, BorderLayout.WEST);
        root.add(mainCards, BorderLayout.CENTER);
        setContentPane(root);
        revalidate();
        repaint();
    }

    private void addNavigation(JPanel sidebar, String label, String key, JPanel panel, boolean selected) {
        JButton button = new ModernButton(navText(key, label));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(226, 46));
        button.putClientProperty("navKey", key);
        navButtons.add(button);
        styleNavButton(button, selected);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setBorder(new EmptyBorder(12, 16, 12, 16));
        button.addActionListener(e -> {
            cardLayout.show(mainCards, key);
            updateNavigation(key);
        });
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(8));
        mainCards.add(panel, key);
        if (selected) {
            cardLayout.show(mainCards, key);
        }
    }

    private void updateNavigation(String key) {
        for (JButton button : navButtons) {
            styleNavButton(button, key.equals(button.getClientProperty("navKey")));
        }
    }

    private static String navText(String key, String label) {
        String icon = switch (key) {
            case "dashboard" -> "•";
            case "employees" -> "•";
            case "projects" -> "•";
            case "tasks" -> "•";
            case "reports" -> "•";
            case "profile" -> "•";
            default -> "•";
        };
        return icon + "  " + label;
    }

    private static void styleNavButton(JButton button, boolean selected) {
        styleButton(button, selected ? SIDEBAR_ACTIVE : SIDEBAR);
        button.setForeground(selected ? Color.WHITE : new Color(203, 213, 225));
        if (button instanceof ModernButton modernButton) {
            modernButton.setButtonColor(selected ? SIDEBAR_ACTIVE : SIDEBAR);
        }
    }

    private JPanel profilePanel() {
        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        info.setBorder(new EmptyBorder(18, 18, 18, 18));
        info.setBackground(CARD);
        info.setText(profileText(currentUser));

        JPanel actions = toolbarPanel();
        JButton refresh = new ModernButton("Tải lại thông tin");
        JButton update = new ModernButton("Cập nhật thông tin");
        JButton changePassword = new ModernButton("Đổi mật khẩu");
        styleButton(refresh, BLUE);
        styleButton(update, GREEN);
        styleButton(changePassword, NAVY);
        actions.add(refresh);
        actions.add(update);
        actions.add(changePassword);

        refresh.addActionListener(e -> {
            Response response = sendSafe(Action.ME, token, Map.of());
            if (response != null && response.isSuccess()) {
                currentUser = (EmployeeView) response.getData();
                info.setText(profileText(currentUser));
                updateUserLabel();
            }
            showResponse(response);
        });
        update.addActionListener(e -> updateProfile(info));
        changePassword.addActionListener(e -> changePassword());

        return pageShell("Tài khoản cá nhân", "Quản lý thông tin đăng nhập và hồ sơ của bạn.",
                actions, styledScroll(info));
    }

    private void showProfileDialog() {
        JTextArea info = new JTextArea(profileText(currentUser));
        info.setEditable(false);
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        info.setOpaque(false);
        JPanel panel = cardPanel(new BorderLayout(0, 14), 18);
        JPanel actions = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton refresh = new ModernButton("Tải lại");
        JButton update = new ModernButton("Cập nhật");
        JButton password = new ModernButton("Đổi mật khẩu");
        styleButton(refresh, BLUE);
        styleButton(update, GREEN);
        styleButton(password, NAVY);
        actions.add(refresh);
        actions.add(update);
        actions.add(password);
        refresh.addActionListener(e -> {
            Response response = sendSafe(Action.ME, token, Map.of());
            if (response != null && response.isSuccess()) {
                currentUser = (EmployeeView) response.getData();
                info.setText(profileText(currentUser));
                updateUserLabel();
            }
            showResponse(response);
        });
        update.addActionListener(e -> updateProfile(info));
        password.addActionListener(e -> changePassword());
        panel.add(info, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, panel, "Hồ sơ tài khoản", JOptionPane.PLAIN_MESSAGE);
    }

    private void updateUserLabel() {
        if (statusLabel != null && currentUser != null) {
            statusLabel.setText("<html>" + currentUser.fullName() + "<br><span style='font-size:10px;color:#cbd5e1'>"
                    + roleLabel(currentUser.role()) + "</span></html>");
        }
    }

    private JPanel employeePanel() {
        JPanel panel = contentPanel();
        panel.add(pageHeader("Nhân viên", "Quản lý thành viên tham gia dự án và trạng thái tài khoản."), BorderLayout.NORTH);

        JPanel body = transparentPanel(new BorderLayout(0, 14));
        body.add(employeeToolbar(), BorderLayout.NORTH);

        JPanel tableCard = cardPanel(new BorderLayout(0, 12), 18);
        employeeStatsPanel = transparentPanel(new GridBagLayout());
        employeeRowsPanel = transparentPanel(new GridBagLayout());
        employeeRangeLabel = new JLabel("Hiển thị 0 / 0 nhân viên");
        employeeRangeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        employeeRangeLabel.setForeground(MUTED);

        JPanel footer = transparentPanel(new BorderLayout(12, 0));
        JPanel pages = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton previous = new ModernButton("< Trước");
        JButton next = new ModernButton("Sau >");
        styleButton(previous, new Color(226, 232, 240));
        styleButton(next, new Color(226, 232, 240));
        previous.addActionListener(e -> {
            if (employeePage > 1) {
                employeePage--;
                renderEmployees();
            }
        });
        next.addActionListener(e -> {
            int maxPage = Math.max(1, (int) Math.ceil(filteredEmployees().size() / (double) EMPLOYEE_PAGE_SIZE));
            if (employeePage < maxPage) {
                employeePage++;
                renderEmployees();
            }
        });
        pages.add(previous);
        pages.add(next);
        footer.add(employeeRangeLabel, BorderLayout.WEST);
        footer.add(pages, BorderLayout.EAST);

        tableCard.add(employeeStatsPanel, BorderLayout.NORTH);
        tableCard.add(employeeRowsPanel, BorderLayout.CENTER);
        tableCard.add(footer, BorderLayout.SOUTH);
        body.add(tableCard, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel employeeToolbar() {
        JPanel toolbar = cardPanel(new GridBagLayout(), 12);
        employeeSearchField = new JTextField();
        employeeSearchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm nhân viên...");
        styleInput(employeeSearchField);
        employeeStatusFilter = new JComboBox<>(new String[]{"Tất cả trạng thái", "Đang hoạt động", "Chờ duyệt", "Ngừng hoạt động"});
        employeeRoleFilter = new JComboBox<>(new String[]{"Tất cả vai trò", "Nhân viên", "Quản lý"});
        JButton add = new ModernButton("+ Thêm nhân viên");
        styleButton(add, BLUE);
        add.addActionListener(e -> createEmployeeDialog());

        DocumentListener searchListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetEmployeePageAndRender();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetEmployeePageAndRender();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetEmployeePageAndRender();
            }
        };
        employeeSearchField.getDocument().addDocumentListener(searchListener);
        employeeStatusFilter.addActionListener(e -> resetEmployeePageAndRender());
        employeeRoleFilter.addActionListener(e -> resetEmployeePageAndRender());

        GridBagConstraints rule = new GridBagConstraints();
        rule.gridy = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.insets = new Insets(0, 0, 0, 10);
        toolbar.add(employeeSearchField, rule);
        rule.gridx = 1;
        rule.weightx = 0;
        toolbar.add(employeeStatusFilter, rule);
        rule.gridx = 2;
        toolbar.add(employeeRoleFilter, rule);
        rule.gridx = 3;
        rule.insets = new Insets(0, 0, 0, 0);
        toolbar.add(add, rule);
        return toolbar;
    }

    private JPanel projectPanel(boolean manager) {
        projectTable = table();
        JPanel actions = toolbarPanel();
        JButton refresh = new ModernButton("Tải lại");
        styleButton(refresh, BLUE);
        actions.add(refresh);
        refresh.addActionListener(e -> refreshProjects());

        if (manager) {
            JButton update = new ModernButton("Sửa dự án");
            JButton assign = new ModernButton("Thêm nhân viên vào dự án");
            styleButton(update, NAVY);
            styleButton(assign, BLUE_DARK);
            actions.add(update);
            actions.add(assign);
            update.addActionListener(e -> updateProject());
            assign.addActionListener(e -> assignProjectMember());
        }

        return pageShell("Thông tin dự án", "Hệ thống chỉ quản lý một dự án duy nhất của công ty.",
                actions, styledScroll(projectTable));
    }

    private JPanel taskPanel(boolean manager) {
        taskTable = table();
        JPanel actions = toolbarPanel();
        JButton refresh = new ModernButton("Tải lại");
        styleButton(refresh, BLUE);
        actions.add(refresh);
        refresh.addActionListener(e -> {
            if (manager) {
                refreshAllTasks();
            } else {
                refreshMyTasks();
            }
        });

        if (manager) {
            JButton create = new ModernButton("Giao công việc");
            styleButton(create, GREEN);
            actions.add(create);
            create.addActionListener(e -> createTask());
        } else {
            JButton progress = new ModernButton("Cập nhật tiến độ");
            JButton done = new ModernButton("Đánh dấu hoàn thành");
            JButton note = new ModernButton("Ghi chú");
            JButton submit = new ModernButton("Nộp kết quả");
            styleButton(progress, BLUE);
            styleButton(done, GREEN);
            styleButton(note, NAVY);
            styleButton(submit, BLUE_DARK);
            actions.add(progress);
            actions.add(done);
            actions.add(note);
            actions.add(submit);
            progress.addActionListener(e -> updateTaskProgress());
            done.addActionListener(e -> completeTask());
            note.addActionListener(e -> addTaskNote());
            submit.addActionListener(e -> submitTaskResult());
        }

        return pageShell(manager ? "Quản lý công việc" : "Công việc của tôi",
                manager ? "Giao việc cho nhân viên và theo dõi tiến độ toàn bộ dự án."
                        : "Theo dõi deadline, cập nhật tiến độ và ghi chú khi đang thực hiện.",
                actions, styledScroll(taskTable));
    }

    private JPanel dashboardPanel() {
        JPanel panel = contentPanel();
        DashboardStats data = loadDashboard();
        ProjectView project = loadPrimaryProject();
        JPanel header = pageHeader("Tổng quan dự án", "Theo dõi tiến độ, công việc và tình trạng phân công của dự án.");

        JPanel stats = new JPanel(new GridBagLayout());
        stats.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 14);
        stats.add(statCard("Tổng công việc", String.valueOf(data.totalTasks()),
                "Thành viên: " + data.totalEmployees(), BLUE, "□"), gbc);
        gbc.gridx = 1;
        stats.add(statCard("Đang thực hiện", String.valueOf(data.inProgressTasks()), "Công việc đang làm", ORANGE, "•"), gbc);
        gbc.gridx = 2;
        stats.add(statCard("Đã hoàn thành", String.valueOf(data.completedTasks()), "Công việc hoàn tất", GREEN, "OK"), gbc);
        gbc.gridx = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        stats.add(statCard("Quá hạn", String.valueOf(data.overdueTasks()), "Cần xử lý ngay", RED, "!"), gbc);

        panel.add(header, BorderLayout.NORTH);
        JPanel body = transparentPanel(new BorderLayout(0, 16));
        body.add(stats, BorderLayout.NORTH);

        JPanel detailGrid = transparentPanel(new GridBagLayout());
        GridBagConstraints detail = new GridBagConstraints();
        detail.gridx = 0;
        detail.gridy = 0;
        detail.weightx = 0.64;
        detail.weighty = 1;
        detail.fill = GridBagConstraints.BOTH;
        detail.insets = new Insets(0, 0, 0, 16);

        JPanel left = transparentPanel(new BorderLayout(0, 16));
        left.add(projectProgressPanel(data), BorderLayout.NORTH);
        left.add(nearDeadlinePanel(data.nearDeadlineTasks()), BorderLayout.CENTER);
        detailGrid.add(left, detail);

        detail.gridx = 1;
        detail.weightx = 0.36;
        detail.insets = new Insets(0, 0, 0, 0);
        detailGrid.add(projectInfoPanel(project, data), detail);

        body.add(detailGrid, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private DashboardStats loadDashboard() {
        Response response = sendSafe(Action.DASHBOARD, token, Map.of());
        if (response != null && response.isSuccess()) {
            return (DashboardStats) response.getData();
        }
        return new DashboardStats(0, 0, 0, 0, 0, 0, 0, List.of());
    }

    private ProjectView loadPrimaryProject() {
        Response response = sendSafe(Action.LIST_PROJECTS, token, Map.of());
        if (response != null && response.isSuccess()) {
            List<ProjectView> projects = castList(response.getData());
            if (!projects.isEmpty()) {
                return projects.getFirst();
            }
        }
        return null;
    }

    private JPanel projectProgressPanel(DashboardStats data) {
        JPanel panel = cardPanel(new BorderLayout(0, 18), 20);
        JLabel title = sectionTitle("Tiến độ dự án");
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(data.projectProgress());
        progress.setStringPainted(false);
        progress.setPreferredSize(new Dimension(0, 14));
        progress.setForeground(GREEN);
        progress.setBackground(new Color(226, 232, 240));

        JLabel percent = new JLabel(data.projectProgress() + "% hoàn thành");
        percent.setFont(new Font("Segoe UI", Font.BOLD, 24));
        percent.setForeground(NAVY);

        JPanel top = transparentPanel(new BorderLayout(0, 8));
        top.add(title, BorderLayout.NORTH);
        top.add(percent, BorderLayout.CENTER);
        top.add(progress, BorderLayout.SOUTH);

        JPanel counters = transparentPanel(new GridBagLayout());
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridy = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.BOTH;
        rule.insets = new Insets(0, 0, 0, 10);
        counters.add(counterPill("Chưa bắt đầu", data.todoTasks(), BLUE), rule);
        rule.gridx = 1;
        counters.add(counterPill("Đang thực hiện", data.inProgressTasks(), ORANGE), rule);
        rule.gridx = 2;
        counters.add(counterPill("Đã hoàn thành", data.completedTasks(), GREEN), rule);
        rule.gridx = 3;
        rule.insets = new Insets(0, 0, 0, 0);
        counters.add(counterPill("Quá hạn", data.overdueTasks(), RED), rule);

        panel.add(top, BorderLayout.NORTH);
        panel.add(counters, BorderLayout.CENTER);
        return panel;
    }

    private JPanel nearDeadlinePanel(List<TaskView> tasks) {
        JPanel panel = cardPanel(new BorderLayout(0, 14), 20);
        panel.add(sectionTitle("Công việc gần deadline"), BorderLayout.NORTH);
        JPanel list = transparentPanel(new GridBagLayout());
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.insets = new Insets(0, 0, 10, 0);
        if (tasks == null || tasks.isEmpty()) {
            JLabel empty = new JLabel("Chưa có công việc sắp đến hạn.");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            empty.setForeground(MUTED);
            list.add(empty, rule);
        } else {
            int row = 0;
            for (TaskView task : tasks) {
                rule.gridy = row++;
                list.add(deadlineRow(task), rule);
            }
        }
        panel.add(list, BorderLayout.CENTER);
        return panel;
    }

    private JPanel projectInfoPanel(ProjectView project, DashboardStats data) {
        JPanel panel = cardPanel(new BorderLayout(0, 16), 20);
        panel.add(sectionTitle("Thông tin dự án"), BorderLayout.NORTH);

        JPanel rows = transparentPanel(new GridBagLayout());
        addInfoRow(rows, 0, "Tên dự án", project == null ? "Chưa có dữ liệu" : project.name());
        addInfoRow(rows, 1, "Ngày bắt đầu", project == null ? "" : display(project.startDate()));
        addInfoRow(rows, 2, "Ngày kết thúc", project == null ? "" : display(project.deadline()));
        addInfoRow(rows, 3, "Manager", currentUser.role() == Role.MANAGER ? currentUser.fullName() : "Quản lý dự án");
        addInfoRow(rows, 4, "Số thành viên", project == null ? "0" : String.valueOf(project.members().size()));
        addInfoRow(rows, 5, "Trạng thái", project == null ? "" : projectStatusLabel(project.status()));
        addInfoRow(rows, 6, "% tiến độ", data.projectProgress() + "%");

        JButton details = new ModernButton("Xem chi tiết dự án");
        styleButton(details, BLUE);
        details.addActionListener(e -> {
            cardLayout.show(mainCards, "projects");
            updateNavigation("projects");
        });

        panel.add(rows, BorderLayout.CENTER);
        panel.add(details, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel reportPanel() {
        DashboardStats data = loadDashboard();
        JPanel report = contentPanel();
        report.add(pageHeader("Báo cáo", "Tổng hợp nhanh tình trạng công việc của dự án."), BorderLayout.NORTH);
        JPanel body = transparentPanel(new BorderLayout(0, 16));
        body.add(projectProgressPanel(data), BorderLayout.NORTH);
        body.add(nearDeadlinePanel(data.nearDeadlineTasks()), BorderLayout.CENTER);
        report.add(body, BorderLayout.CENTER);
        return report;
    }

    private static JLabel sectionTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(NAVY);
        return title;
    }

    private static JPanel counterPill(String label, long value, Color color) {
        JPanel panel = new RoundedPanel(new BorderLayout(0, 3), tint(color), new Color(color.getRed(), color.getGreen(), color.getBlue(), 70), 18);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));
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

    private static JPanel deadlineRow(TaskView task) {
        JPanel row = new RoundedPanel(new BorderLayout(12, 0), new Color(248, 250, 252), BORDER, 16);
        row.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel text = transparentPanel(new BorderLayout(0, 4));
        JLabel title = new JLabel(task.title());
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(NAVY);
        JLabel meta = new JLabel("Người thực hiện: " + nullSafe(task.assigneeName()) + "  •  Deadline: " + display(task.deadline()));
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        meta.setForeground(MUTED);
        text.add(title, BorderLayout.NORTH);
        text.add(meta, BorderLayout.CENTER);

        JPanel badges = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badges.add(badge(priorityLabel(task.priority()), priorityColor(task.priority())));
        badges.add(badge(taskStatusLabel(String.valueOf(task.status())), statusColor(String.valueOf(task.status()))));
        row.add(text, BorderLayout.CENTER);
        row.add(badges, BorderLayout.EAST);
        return row;
    }

    private static JLabel badge(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setForeground(color.darker());
        label.setBackground(tint(color));
        label.setBorder(new EmptyBorder(6, 10, 6, 10));
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return label;
    }

    private static Color priorityColor(TaskPriority priority) {
        if (priority == null) {
            return BLUE;
        }
        return switch (priority) {
            case LOW -> BLUE;
            case MEDIUM -> ORANGE;
            case HIGH, URGENT -> RED;
        };
    }

    private static Color statusColor(String status) {
        if ("COMPLETED".equals(status) || "DONE".equals(status)) {
            return GREEN;
        }
        if ("IN_PROGRESS".equals(status)) {
            return ORANGE;
        }
        if ("CANCELLED".equals(status)) {
            return RED;
        }
        return BLUE;
    }

    private static String taskStatusLabel(String status) {
        return switch (status) {
            case "TODO" -> "Chưa bắt đầu";
            case "IN_PROGRESS" -> "Đang thực hiện";
            case "COMPLETED", "DONE" -> "Đã hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> "Chưa rõ";
        };
    }

    private static String priorityLabel(TaskPriority priority) {
        if (priority == null) {
            return "Trung bình";
        }
        return switch (priority) {
            case LOW -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH -> "Cao";
            case URGENT -> "Khẩn cấp";
        };
    }

    private static String projectStatusLabel(ProjectStatus status) {
        if (status == null) {
            return "Chưa rõ";
        }
        return switch (status) {
            case PLANNING -> "Lên kế hoạch";
            case IN_PROGRESS -> "Đang thực hiện";
            case COMPLETED -> "Đã hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private static String statusLabel(AccountStatus status) {
        if (status == null) {
            return "Chưa xác định";
        }
        return switch (status) {
            case ACTIVE -> "Đang hoạt động";
            case PENDING -> "Chờ duyệt";
            case DISABLED -> "Ngừng hoạt động";
        };
    }

    private static Color statusColor(AccountStatus status) {
        if (status == null) {
            return MUTED;
        }
        return switch (status) {
            case ACTIVE -> GREEN;
            case PENDING -> ORANGE;
            case DISABLED -> RED;
        };
    }

    private static String roleLabel(Role role) {
        if (role == Role.MANAGER) {
            return "Quản lý";
        }
        return "Nhân viên";
    }

    private static Color roleColor(Role role) {
        return role == Role.MANAGER ? new Color(124, 58, 237) : BLUE;
    }

    private static void addInfoRow(JPanel rows, int row, String label, String value) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(0, 0, 12, 16);
        JLabel labelView = new JLabel(label);
        labelView.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelView.setForeground(MUTED);
        rows.add(labelView, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = GridBagConstraints.WEST;
        right.insets = new Insets(0, 0, 12, 0);
        JLabel valueView = new JLabel(nullSafe(value));
        valueView.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueView.setForeground(NAVY);
        rows.add(valueView, right);
    }

    private static String display(LocalDate date) {
        return date == null ? "Chưa đặt" : String.valueOf(date);
    }

    private static String nullSafe(String text) {
        return text == null || text.isBlank() ? "Chưa có" : text;
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
        header.setBorder(new EmptyBorder(24, 26, 24, 26));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 27));
        titleLabel.setForeground(Color.WHITE);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(219, 234, 254));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.CENTER);
        return header;
    }

    private static JPanel statCard(String label, String value, String note, Color accent) {
        return statCard(label, value, note, accent, "");
    }

    private static JPanel statCard(String label, String value, String note, Color accent, String icon) {
        Color fill = tint(accent);
        JPanel card = new RoundedPanel(new BorderLayout(0, 10), fill, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80), 22);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel labelView = new JLabel(label);
        labelView.setFont(LABEL_FONT);
        labelView.setForeground(accent.darker());
        JLabel valueView = new JLabel(value);
        valueView.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueView.setForeground(NAVY);
        JLabel noteView = new JLabel(note);
        noteView.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteView.setForeground(new Color(71, 85, 105));
        JPanel text = transparentPanel(new BorderLayout(0, 4));
        text.add(labelView, BorderLayout.NORTH);
        text.add(valueView, BorderLayout.CENTER);
        text.add(noteView, BorderLayout.SOUTH);
        JLabel iconView = new JLabel(icon, JLabel.CENTER);
        iconView.setFont(new Font("Segoe UI", Font.BOLD, 18));
        iconView.setForeground(accent.darker());
        iconView.setPreferredSize(new Dimension(42, 42));
        card.add(text, BorderLayout.CENTER);
        card.add(iconView, BorderLayout.EAST);
        return card;
    }

    private static JPanel stepCard(String number, String title, String detail) {
        JPanel card = new RoundedPanel(new BorderLayout(0, 8), new Color(248, 250, 252), BORDER, 18);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel num = new JLabel(number);
        num.setFont(new Font("Segoe UI", Font.BOLD, 12));
        num.setForeground(BLUE);
        JLabel titleView = new JLabel(title);
        titleView.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleView.setForeground(NAVY);
        JTextArea detailView = new JTextArea(detail);
        detailView.setLineWrap(true);
        detailView.setWrapStyleWord(true);
        detailView.setEditable(false);
        detailView.setOpaque(false);
        detailView.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailView.setForeground(MUTED);
        JPanel top = transparentPanel(new BorderLayout(0, 3));
        top.add(num, BorderLayout.NORTH);
        top.add(titleView, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);
        card.add(detailView, BorderLayout.CENTER);
        return card;
    }

    private static JPanel brandPanel() {
        JPanel panel = transparentPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(250, 58));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel logo = new JLabel("", JLabel.CENTER);
        ImageIcon icon = logoIcon(48);
        if (icon != null) {
            logo.setIcon(icon);
        } else {
            logo.setText("HT");
            logo.setOpaque(true);
            logo.setBackground(BLUE);
            logo.setForeground(Color.WHITE);
            logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        }
        logo.setPreferredSize(new Dimension(50, 50));
        logo.setMinimumSize(new Dimension(50, 50));
        logo.setMaximumSize(new Dimension(50, 50));
        JPanel text = transparentPanel(new BorderLayout(0, 2));
        text.setPreferredSize(new Dimension(184, 52));
        JLabel brand = new JLabel("TASKFLOW");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brand.setForeground(Color.WHITE);
        JLabel caption = new JLabel("Quản lý phân công công việc");
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        caption.setForeground(new Color(148, 163, 184));
        text.add(brand, BorderLayout.NORTH);
        text.add(caption, BorderLayout.CENTER);
        GridBagConstraints logoRule = new GridBagConstraints();
        logoRule.gridx = 0;
        logoRule.gridy = 0;
        logoRule.anchor = GridBagConstraints.WEST;
        logoRule.insets = new Insets(0, 0, 0, 12);
        panel.add(logo, logoRule);
        GridBagConstraints textRule = new GridBagConstraints();
        textRule.gridx = 1;
        textRule.gridy = 0;
        textRule.weightx = 1;
        textRule.fill = GridBagConstraints.HORIZONTAL;
        textRule.anchor = GridBagConstraints.WEST;
        panel.add(text, textRule);
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
            Image image = cropped.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(image);
        } catch (Exception e) {
            Image image = new ImageIcon(url).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(image);
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
        if (!found) {
            return image;
        }
        int padding = 0;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(image.getWidth() - 1, maxX + padding);
        maxY = Math.min(image.getHeight() - 1, maxY + padding);
        return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static Color tint(Color color) {
        return new Color(
                Math.min(255, color.getRed() + 210),
                Math.min(255, color.getGreen() + 205),
                Math.min(255, color.getBlue() + 190)
        );
    }

    private void updateProfile(JTextArea info) {
        JTextField fullName = new JTextField(currentUser.fullName());
        JTextField email = new JTextField(currentUser.email());
        JTextField phone = new JTextField(currentUser.phone());
        JPanel panel = formPanel(new String[]{"Họ tên", "Email", "Số điện thoại"},
                new java.awt.Component[]{fullName, email, phone});
        if (confirm(panel, "Cập nhật thông tin")) {
            Response response = sendSafe(Action.UPDATE_PROFILE, token, map(
                    "fullName", fullName.getText(),
                    "email", email.getText(),
                    "phone", phone.getText()
            ));
            if (response != null && response.isSuccess()) {
                currentUser = (EmployeeView) response.getData();
                info.setText(profileText(currentUser));
                updateUserLabel();
            }
            showResponse(response);
        }
    }

    private void changePassword() {
        JPasswordField oldPassword = new JPasswordField();
        JPasswordField newPassword = new JPasswordField();
        JPanel panel = formPanel(new String[]{"Mật khẩu cũ", "Mật khẩu mới"},
                new java.awt.Component[]{oldPassword, newPassword});
        if (confirm(panel, "Đổi mật khẩu")) {
            Response response = sendSafe(Action.CHANGE_PASSWORD, token, map(
                    "oldPassword", new String(oldPassword.getPassword()),
                    "newPassword", new String(newPassword.getPassword())
            ));
            showResponse(response);
        }
    }

    private void refreshEmployees() {
        Response response = sendSafe(Action.LIST_EMPLOYEE_DIRECTORY, token, Map.of());
        if (response == null || !response.isSuccess()) {
            showResponse(response);
            return;
        }
        employeeDirectory.clear();
        employeeDirectory.addAll(castList(response.getData()));
        employeePage = 1;
        renderEmployees();
    }

    private void employeeAction(Action action, String field) {
        Long id = selectedId(employeeTable);
        if (id == null) {
            message("Hãy chọn một nhân viên trong bảng.");
            return;
        }
        Response response = sendSafe(action, token, map(field, id));
        showResponse(response);
        refreshEmployees();
    }

    private void resetEmployeePageAndRender() {
        employeePage = 1;
        renderEmployees();
    }

    private List<EmployeeDirectoryView> filteredEmployees() {
        String keyword = employeeSearchField == null ? "" : employeeSearchField.getText().trim().toLowerCase();
        String statusFilter = employeeStatusFilter == null ? "Tất cả trạng thái" : String.valueOf(employeeStatusFilter.getSelectedItem());
        String roleFilter = employeeRoleFilter == null ? "Tất cả vai trò" : String.valueOf(employeeRoleFilter.getSelectedItem());
        return employeeDirectory.stream()
                .filter(row -> {
                    EmployeeView employee = row.employee();
                    String haystack = (nullSafe(employee.fullName()) + " " + nullSafe(employee.username()) + " " + nullSafe(employee.email())).toLowerCase();
                    boolean matchesKeyword = keyword.isBlank() || haystack.contains(keyword);
                    boolean matchesStatus = "Tất cả trạng thái".equals(statusFilter) || statusLabel(employee.status()).equals(statusFilter);
                    boolean matchesRole = "Tất cả vai trò".equals(roleFilter) || roleLabel(employee.role()).equals(roleFilter);
                    return matchesKeyword && matchesStatus && matchesRole;
                })
                .toList();
    }

    private void renderEmployees() {
        if (employeeRowsPanel == null || employeeStatsPanel == null) {
            return;
        }
        renderEmployeeStats();
        employeeRowsPanel.removeAll();
        List<EmployeeDirectoryView> filtered = filteredEmployees();
        int total = filtered.size();
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) EMPLOYEE_PAGE_SIZE));
        employeePage = Math.min(employeePage, maxPage);
        int from = total == 0 ? 0 : (employeePage - 1) * EMPLOYEE_PAGE_SIZE;
        int to = Math.min(total, from + EMPLOYEE_PAGE_SIZE);

        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.insets = new Insets(0, 0, 8, 0);

        if (total == 0) {
            employeeRowsPanel.add(employeeEmptyState(), rule);
            employeeRangeLabel.setText("Hiển thị 0 / 0 nhân viên");
        } else {
            employeeRowsPanel.add(employeeHeaderRow(), rule);
            int gridY = 1;
            for (EmployeeDirectoryView row : filtered.subList(from, to)) {
                rule.gridy = gridY++;
                employeeRowsPanel.add(employeeRow(row), rule);
            }
            employeeRangeLabel.setText("Hiển thị " + (from + 1) + "-" + to + " / " + total + " nhân viên");
        }
        employeeRowsPanel.revalidate();
        employeeRowsPanel.repaint();
    }

    private void renderEmployeeStats() {
        employeeStatsPanel.removeAll();
        long total = employeeDirectory.size();
        long active = employeeDirectory.stream().filter(row -> row.employee().status() == AccountStatus.ACTIVE).count();
        long pending = employeeDirectory.stream().filter(row -> row.employee().status() == AccountStatus.PENDING).count();
        long disabled = employeeDirectory.stream().filter(row -> row.employee().status() == AccountStatus.DISABLED).count();
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridy = 0;
        rule.weightx = 1;
        rule.fill = GridBagConstraints.BOTH;
        rule.insets = new Insets(0, 0, 12, 10);
        employeeStatsPanel.add(statCard("Tổng thành viên", String.valueOf(total), "Thuộc dự án", BLUE, "NV"), rule);
        rule.gridx = 1;
        employeeStatsPanel.add(statCard("Đang hoạt động", String.valueOf(active), "Có thể đăng nhập", GREEN, "✓"), rule);
        rule.gridx = 2;
        employeeStatsPanel.add(statCard("Chờ duyệt", String.valueOf(pending), "Đợi quản lý duyệt", ORANGE, "..."), rule);
        rule.gridx = 3;
        rule.insets = new Insets(0, 0, 12, 0);
        employeeStatsPanel.add(statCard("Ngừng hoạt động", String.valueOf(disabled), "Tài khoản bị khóa", RED, "!"), rule);
        employeeStatsPanel.revalidate();
        employeeStatsPanel.repaint();
    }

    private JPanel employeeHeaderRow() {
        JPanel header = transparentPanel(new GridBagLayout());
        header.setBorder(new EmptyBorder(0, 8, 4, 8));
        addEmployeeCell(header, 0, "Nhân viên", 0.30, true);
        addEmployeeCell(header, 1, "Liên hệ", 0.25, true);
        addEmployeeCell(header, 2, "Vai trò", 0.13, true);
        addEmployeeCell(header, 3, "Trạng thái", 0.16, true);
        addEmployeeCell(header, 4, "Công việc", 0.12, true);
        addEmployeeCell(header, 5, "Thao tác", 0.04, true);
        return header;
    }

    private JPanel employeeRow(EmployeeDirectoryView row) {
        EmployeeView employee = row.employee();
        JPanel panel = new RoundedPanel(new GridBagLayout(), Color.WHITE, BORDER, 14);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        addEmployeeComponent(panel, 0, employeeIdentity(employee), 0.30);
        addEmployeeComponent(panel, 1, contactBlock(employee), 0.25);
        addEmployeeComponent(panel, 2, badge(roleLabel(employee.role()), roleColor(employee.role())), 0.13);
        addEmployeeComponent(panel, 3, badge(statusLabel(employee.status()), statusColor(employee.status())), 0.16);
        addEmployeeComponent(panel, 4, taskCountBlock(row), 0.12);
        JButton menu = new ModernButton("⋮");
        styleButton(menu, new Color(226, 232, 240));
        menu.setPreferredSize(new Dimension(42, 38));
        menu.addActionListener(e -> employeeActionMenu(menu, row));
        addEmployeeComponent(panel, 5, menu, 0.04);
        return panel;
    }

    private JPanel employeeIdentity(EmployeeView employee) {
        JPanel panel = transparentPanel(new BorderLayout(10, 0));
        panel.add(new AvatarLabel(initials(employee.fullName())), BorderLayout.WEST);
        JPanel text = transparentPanel(new BorderLayout(0, 2));
        JLabel name = new JLabel(nullSafe(employee.fullName()));
        name.setFont(new Font("Segoe UI", Font.BOLD, 14));
        name.setForeground(NAVY);
        JLabel username = new JLabel("@" + nullSafe(employee.username()));
        username.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        username.setForeground(MUTED);
        text.add(name, BorderLayout.NORTH);
        text.add(username, BorderLayout.CENTER);
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    private JPanel contactBlock(EmployeeView employee) {
        JPanel panel = transparentPanel(new BorderLayout(0, 2));
        JLabel email = new JLabel(nullSafe(employee.email()));
        email.setToolTipText(nullSafe(employee.email()));
        email.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        email.setForeground(NAVY);
        JLabel phone = new JLabel(nullSafe(employee.phone()));
        phone.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        phone.setForeground(MUTED);
        panel.add(email, BorderLayout.NORTH);
        panel.add(phone, BorderLayout.CENTER);
        return panel;
    }

    private JPanel taskCountBlock(EmployeeDirectoryView row) {
        JPanel panel = transparentPanel(new BorderLayout(0, 2));
        JLabel progress = new JLabel(row.inProgressTasks() + " đang làm");
        progress.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progress.setForeground(ORANGE.darker());
        JLabel done = new JLabel(row.completedTasks() + " hoàn thành");
        done.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        done.setForeground(GREEN.darker());
        panel.add(progress, BorderLayout.NORTH);
        panel.add(done, BorderLayout.CENTER);
        return panel;
    }

    private JPanel employeeEmptyState() {
        JPanel empty = transparentPanel(new GridBagLayout());
        empty.setBorder(new EmptyBorder(34, 20, 34, 20));
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = 0;
        rule.fill = GridBagConstraints.HORIZONTAL;
        JLabel icon = new JLabel("NV", JLabel.CENTER);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 22));
        icon.setForeground(BLUE);
        empty.add(icon, rule);
        rule.gridy = 1;
        JLabel title = new JLabel("Chưa có nhân viên", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(NAVY);
        rule.insets = new Insets(8, 0, 0, 0);
        empty.add(title, rule);
        rule.gridy = 2;
        JLabel note = new JLabel("Thêm nhân viên vào dự án để bắt đầu phân công công việc.", JLabel.CENTER);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        note.setForeground(MUTED);
        rule.insets = new Insets(6, 0, 14, 0);
        empty.add(note, rule);
        rule.gridy = 3;
        JButton add = new ModernButton("+ Thêm nhân viên");
        styleButton(add, BLUE);
        add.addActionListener(e -> createEmployeeDialog());
        empty.add(add, rule);
        return empty;
    }

    private static void addEmployeeCell(JPanel panel, int col, String text, double weight, boolean header) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, header ? 12 : 13));
        label.setForeground(header ? MUTED : NAVY);
        addEmployeeComponent(panel, col, label, weight);
    }

    private static void addEmployeeComponent(JPanel panel, int col, Component component, double weight) {
        GridBagConstraints rule = new GridBagConstraints();
        rule.gridx = col;
        rule.gridy = 0;
        rule.weightx = weight;
        rule.fill = GridBagConstraints.HORIZONTAL;
        rule.anchor = GridBagConstraints.CENTER;
        rule.insets = new Insets(0, 0, 0, col == 5 ? 0 : 12);
        panel.add(component, rule);
    }

    private void employeeActionMenu(JButton owner, EmployeeDirectoryView row) {
        EmployeeView employee = row.employee();
        JPopupMenu menu = new JPopupMenu();
        JMenuItem detail = new JMenuItem("Xem chi tiết");
        detail.addActionListener(e -> showEmployeeDetail(row));
        menu.add(detail);

        if (employee.status() == AccountStatus.ACTIVE) {
            JMenuItem edit = new JMenuItem("Chỉnh sửa");
            edit.addActionListener(e -> editEmployeeDialog(employee));
            menu.add(edit);
            if (!employee.id().equals(currentUser.id())) {
                JMenuItem disable = new JMenuItem("Khóa tài khoản");
                disable.addActionListener(e -> confirmEmployeeStatus(employee, Action.DISABLE_EMPLOYEE));
                menu.add(disable);
            }
        } else if (employee.status() == AccountStatus.PENDING) {
            JMenuItem approve = new JMenuItem("Duyệt tài khoản");
            approve.addActionListener(e -> confirmEmployeeStatus(employee, Action.APPROVE_EMPLOYEE));
            menu.add(approve);
        } else if (employee.status() == AccountStatus.DISABLED) {
            JMenuItem enable = new JMenuItem("Mở khóa / Kích hoạt lại");
            enable.addActionListener(e -> confirmEmployeeStatus(employee, Action.ENABLE_EMPLOYEE));
            menu.add(enable);
        }
        menu.show(owner, 0, owner.getHeight());
    }

    private void showEmployeeDetail(EmployeeDirectoryView row) {
        EmployeeView employee = row.employee();
        JPanel panel = cardPanel(new BorderLayout(0, 14), 18);
        panel.add(employeeIdentity(employee), BorderLayout.NORTH);
        JTextArea detail = new JTextArea("""
                Email       : %s
                Số điện thoại: %s
                Vai trò      : %s
                Trạng thái   : %s
                Công việc    : %s đang làm, %s hoàn thành
                """.formatted(nullSafe(employee.email()), nullSafe(employee.phone()), roleLabel(employee.role()),
                statusLabel(employee.status()), row.inProgressTasks(), row.completedTasks()));
        detail.setEditable(false);
        detail.setOpaque(false);
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        panel.add(detail, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, panel, "Chi tiết nhân viên", JOptionPane.PLAIN_MESSAGE);
    }

    private void confirmEmployeeStatus(EmployeeView employee, Action action) {
        String title;
        String message;
        String buttonText;
        if (action == Action.APPROVE_EMPLOYEE) {
            title = "Duyệt tài khoản?";
            message = "Nhân viên " + employee.fullName() + " sẽ có thể tham gia và nhận công việc trong dự án.";
            buttonText = "Duyệt tài khoản";
        } else if (action == Action.DISABLE_EMPLOYEE) {
            title = "Khóa tài khoản?";
            message = "Nhân viên sẽ không thể tiếp tục truy cập hệ thống cho đến khi được mở khóa.";
            buttonText = "Khóa tài khoản";
        } else {
            title = "Mở khóa tài khoản?";
            message = "Nhân viên " + employee.fullName() + " sẽ có thể đăng nhập và tiếp tục công việc.";
            buttonText = "Mở khóa";
        }
        JPanel panel = cardPanel(new BorderLayout(0, 12), 18);
        JLabel text = new JLabel("<html><b>" + title + "</b><br>" + message + "</html>");
        text.setFont(BODY_FONT);
        text.setForeground(NAVY);
        panel.add(text, BorderLayout.CENTER);
        int choice = JOptionPane.showOptionDialog(this, panel, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Hủy", buttonText}, "Hủy");
        if (choice == 1) {
            Response response = sendSafe(action, token, map("employeeId", employee.id()));
            showResponse(response);
            refreshEmployees();
        }
    }

    private void createEmployeeDialog() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField fullName = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JPanel panel = formPanel(new String[]{"Tên đăng nhập", "Mật khẩu", "Họ tên", "Email", "Số điện thoại"},
                new Component[]{username, password, fullName, email, phone});
        if (confirm(panel, "Thêm nhân viên")) {
            Response response = sendSafe(Action.CREATE_EMPLOYEE, token, map(
                    "username", username.getText(),
                    "password", new String(password.getPassword()),
                    "fullName", fullName.getText(),
                    "email", email.getText(),
                    "phone", phone.getText()
            ));
            showResponse(response);
            refreshEmployees();
            refreshProjects();
        }
    }

    private void editEmployeeDialog(EmployeeView employee) {
        JTextField fullName = new JTextField(employee.fullName());
        JTextField email = new JTextField(employee.email());
        JTextField phone = new JTextField(employee.phone());
        JComboBox<String> role = new JComboBox<>(new String[]{"Nhân viên", "Quản lý"});
        role.setSelectedItem(roleLabel(employee.role()));
        JPanel panel = formPanel(new String[]{"Họ tên", "Email", "Số điện thoại", "Vai trò"},
                new Component[]{fullName, email, phone, role});
        if (confirm(panel, "Chỉnh sửa nhân viên")) {
            Response response = sendSafe(Action.UPDATE_EMPLOYEE, token, map(
                    "employeeId", employee.id(),
                    "fullName", fullName.getText(),
                    "email", email.getText(),
                    "phone", phone.getText(),
                    "role", "Quản lý".equals(role.getSelectedItem()) ? Role.MANAGER : Role.EMPLOYEE
            ));
            showResponse(response);
            refreshEmployees();
        }
    }

    private void refreshProjects() {
        Response response = sendSafe(Action.LIST_PROJECTS, token, Map.of());
        if (response == null || !response.isSuccess()) {
            showResponse(response);
            return;
        }
        DefaultTableModel model = model("ID", "Tên dự án", "Mô tả", "Bắt đầu", "Deadline", "Trạng thái", "Thành viên");
        List<ProjectView> projects = castList(response.getData());
        for (ProjectView project : projects) {
            String members = project.members().stream()
                    .map(member -> "#" + member.id() + " " + member.fullName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            model.addRow(new Object[]{project.id(), project.name(), project.description(), project.startDate(),
                    project.deadline(), project.status(), members});
        }
        projectTable.setModel(model);
    }

    private void createProject() {
        ProjectFields fields = projectFields(null);
        if (confirm(fields.panel(), "Cập nhật thông tin dự án")) {
            Response response = sendSafe(Action.CREATE_PROJECT, token, fields.toMap(false));
            showResponse(response);
            refreshProjects();
        }
    }

    private void updateProject() {
        Long id = selectedId(projectTable);
        if (id == null) {
            message("Hãy chọn một dự án trong bảng.");
            return;
        }
        ProjectFields fields = projectFields(projectTable.getSelectedRow());
        if (confirm(fields.panel(), "Sửa dự án")) {
            Response response = sendSafe(Action.UPDATE_PROJECT, token, fields.toMap(true, id));
            showResponse(response);
            refreshProjects();
        }
    }

    private void deleteProject() {
        Long id = selectedId(projectTable);
        if (id == null) {
            message("Hãy chọn một dự án trong bảng.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Xóa dự án #" + id + "?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Response response = sendSafe(Action.DELETE_PROJECT, token, map("projectId", id));
            showResponse(response);
            refreshProjects();
            refreshAllTasks();
        }
    }

    private void assignProjectMember() {
        Long projectId = selectedId(projectTable);
        if (projectId == null) {
            message("Hãy chọn dòng thông tin dự án trong bảng.");
            return;
        }
        JTextField employeeId = new JTextField();
        JPanel panel = formPanel(new String[]{"Mã nhân viên"}, new java.awt.Component[]{employeeId});
        if (confirm(panel, "Thêm nhân viên vào dự án")) {
            Response response = sendSafe(Action.ASSIGN_PROJECT_MEMBER, token, map(
                    "projectId", projectId,
                    "employeeId", parseLong(employeeId.getText())
            ));
            showResponse(response);
            refreshProjects();
        }
    }

    private void refreshMyTasks() {
        Response response = sendSafe(Action.MY_TASKS, token, Map.of());
        fillTaskTable(response);
    }

    private void refreshAllTasks() {
        Response response = sendSafe(Action.LIST_TASKS, token, Map.of());
        fillTaskTable(response);
    }

    private void fillTaskTable(Response response) {
        if (response == null || !response.isSuccess()) {
            showResponse(response);
            return;
        }
        DefaultTableModel model = model("ID", "Công việc", "Nhân viên", "Người giao", "Deadline",
                "Ưu tiên", "Tiến độ", "Trạng thái", "Quá hạn", "Kết quả", "Ghi chú");
        List<TaskView> tasks = castList(response.getData());
        for (TaskView task : tasks) {
            model.addRow(new Object[]{task.id(), task.title(), task.assigneeName(), task.assignerName(),
                    task.deadline(), task.priority(), task.progress() + "%", task.status(),
                    task.overdue() ? "Quá hạn" : "", task.resultText(), task.note()});
        }
        taskTable.setModel(model);
    }

    private void createTask() {
        List<EmployeeOption> employees = activeEmployeeOptions();
        if (employees.isEmpty()) {
            message("Chưa có nhân viên đang hoạt động để giao công việc.");
            return;
        }
        JComboBox<EmployeeOption> assignee = new JComboBox<>(employees.toArray(new EmployeeOption[0]));
        JTextField title = new JTextField();
        JTextArea description = new JTextArea(4, 24);
        JTextField startDate = new JTextField();
        JTextField deadline = new JTextField();
        JComboBox<PriorityOption> priority = new JComboBox<>(new PriorityOption[]{
                new PriorityOption(TaskPriority.LOW, "Thấp"),
                new PriorityOption(TaskPriority.MEDIUM, "Trung bình"),
                new PriorityOption(TaskPriority.HIGH, "Cao"),
                new PriorityOption(TaskPriority.URGENT, "Khẩn cấp")
        });
        priority.setSelectedIndex(1);
        JPanel panel = formPanel(new String[]{"Nhân viên thực hiện", "Tên công việc", "Mô tả",
                        "Ngày bắt đầu yyyy-MM-dd", "Deadline yyyy-MM-dd", "Mức độ ưu tiên"},
                new java.awt.Component[]{assignee, title, new JScrollPane(description), startDate, deadline, priority});
        if (confirm(panel, "Giao công việc")) {
            EmployeeOption selectedEmployee = (EmployeeOption) assignee.getSelectedItem();
            PriorityOption selectedPriority = (PriorityOption) priority.getSelectedItem();
            if (selectedEmployee == null) {
                message("Hãy chọn nhân viên thực hiện.");
                return;
            }
            if (title.getText().trim().isBlank()) {
                message("Tên công việc không được để trống.");
                return;
            }
            Response response = sendSafe(Action.CREATE_TASK, token, map(
                    "projectId", 0L,
                    "assigneeId", selectedEmployee.id(),
                    "title", title.getText(),
                    "description", description.getText(),
                    "startDate", parseDate(startDate.getText()),
                    "deadline", parseDate(deadline.getText()),
                    "priority", selectedPriority == null ? TaskPriority.MEDIUM : selectedPriority.priority()
            ));
            showResponse(response);
            refreshAllTasks();
            refreshProjects();
        }
    }

    private List<EmployeeOption> activeEmployeeOptions() {
        Response response = sendSafe(Action.LIST_EMPLOYEES, token, Map.of());
        if (response == null || !response.isSuccess()) {
            showResponse(response);
            return List.of();
        }
        List<EmployeeView> employees = castList(response.getData());
        return employees.stream()
                .filter(employee -> employee.role() == Role.EMPLOYEE && employee.status() == AccountStatus.ACTIVE)
                .map(employee -> new EmployeeOption(employee.id(), employee.fullName(), employee.username()))
                .toList();
    }

    private void updateTaskProgress() {
        Long taskId = selectedId(taskTable);
        if (taskId == null) {
            message("Hãy chọn một công việc trong bảng.");
            return;
        }
        JTextField progress = new JTextField();
        JPanel panel = formPanel(new String[]{"Tiến độ 0-100"}, new java.awt.Component[]{progress});
        if (confirm(panel, "Cập nhật tiến độ")) {
            Response response = sendSafe(Action.UPDATE_TASK_PROGRESS, token, map(
                    "taskId", taskId,
                    "progress", Integer.parseInt(progress.getText().trim())
            ));
            showResponse(response);
            refreshMyTasks();
        }
    }

    private void completeTask() {
        Long taskId = selectedId(taskTable);
        if (taskId == null) {
            message("Hãy chọn một công việc trong bảng.");
            return;
        }
        Response response = sendSafe(Action.COMPLETE_TASK, token, map("taskId", taskId));
        showResponse(response);
        refreshMyTasks();
    }

    private void addTaskNote() {
        Long taskId = selectedId(taskTable);
        if (taskId == null) {
            message("Hãy chọn một công việc trong bảng.");
            return;
        }
        JTextArea note = new JTextArea(5, 25);
        if (confirm(new JScrollPane(note), "Ghi chú công việc")) {
            Response response = sendSafe(Action.ADD_TASK_NOTE, token, map(
                    "taskId", taskId,
                    "note", note.getText()
            ));
            showResponse(response);
            refreshMyTasks();
        }
    }

    private void submitTaskResult() {
        Long taskId = selectedId(taskTable);
        if (taskId == null) {
            message("Hãy chọn một công việc trong bảng.");
            return;
        }
        JTextArea result = new JTextArea(5, 26);
        JTextField link = new JTextField();
        JTextArea note = new JTextArea(3, 26);
        JPanel panel = formPanel(new String[]{"Nội dung kết quả", "Link kết quả", "Ghi chú"},
                new java.awt.Component[]{new JScrollPane(result), link, new JScrollPane(note)});
        if (confirm(panel, "Nộp kết quả công việc")) {
            Response response = sendSafe(Action.SUBMIT_TASK_RESULT, token, map(
                    "taskId", taskId,
                    "resultText", result.getText(),
                    "resultLink", link.getText(),
                    "note", note.getText()
            ));
            showResponse(response);
            refreshMyTasks();
        }
    }

    private void logout() {
        sendSafe(Action.LOGOUT, token, Map.of());
        token = null;
        currentUser = null;
        showLoginScreen();
    }

    private Response sendSafe(Action action, String token, Map<String, Serializable> data) {
        try {
            output.writeObject(new Request(action, token, data));
            output.flush();
            return (Response) input.readObject();
        } catch (Exception e) {
        return Response.error("Không kết nối được server: " + e.getMessage());
        }
    }

    private boolean showResponse(Response response) {
        if (response == null) {
            message("Không có phản hồi từ server.");
            return false;
        }
        JOptionPane.showMessageDialog(this, response.getMessage(),
                response.isSuccess() ? "Thông báo" : "Lỗi",
                response.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        return response.isSuccess();
    }

    private static JTable table() {
        JTable table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(36);
        table.setFont(BODY_FONT);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(NAVY);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(NAVY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        DefaultTableCellRenderer renderer = new ZebraRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        return table;
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private Long selectedId(JTable table) {
        if (table == null || table.getSelectedRow() < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object value = table.getModel().getValueAt(modelRow, 0);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private ProjectFields projectFields(Integer selectedRow) {
        JTextField name = new JTextField();
        JTextArea description = new JTextArea(4, 24);
        JTextField startDate = new JTextField();
        JTextField deadline = new JTextField();
        JComboBox<ProjectStatus> status = new JComboBox<>(ProjectStatus.values());
        if (selectedRow != null && selectedRow >= 0) {
            int row = projectTable.convertRowIndexToModel(selectedRow);
            name.setText(textAt(projectTable, row, 1));
            description.setText(textAt(projectTable, row, 2));
            startDate.setText(textAt(projectTable, row, 3));
            deadline.setText(textAt(projectTable, row, 4));
            status.setSelectedItem(ProjectStatus.valueOf(textAt(projectTable, row, 5)));
        }
        JPanel panel = formPanel(new String[]{"Tên dự án", "Mô tả", "Ngày bắt đầu yyyy-MM-dd", "Deadline yyyy-MM-dd", "Trạng thái"},
                new java.awt.Component[]{name, new JScrollPane(description), startDate, deadline, status});
        return new ProjectFields(panel, name, description, startDate, deadline, status);
    }

    private static String textAt(JTable table, int row, int col) {
        Object value = table.getModel().getValueAt(row, col);
        return value == null ? "" : String.valueOf(value);
    }

    private static JPanel formPanel(String[] labels, java.awt.Component[] fields) {
        JPanel panel = transparentPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(6, 4, 6, 4));
        for (int i = 0; i < labels.length; i++) {
            addField(panel, i, labels[i], fields[i]);
        }
        return panel;
    }

    private static void addField(JPanel panel, int row, String label, java.awt.Component field) {
        styleFieldIfPossible(field);
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_END;
        left.insets = new Insets(8, 6, 8, 12);
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(LABEL_FONT);
        labelComponent.setForeground(NAVY);
        panel.add(labelComponent, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(8, 0, 8, 6);
        panel.add(field, right);
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
        JLabel title = new JLabel("Quản lý phân công công việc");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(226, 232, 240));
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(title, gbc);

        gbc.gridy = 2;
        JTextArea copy = new JTextArea("Theo dõi dự án, giao việc cho nhân viên, cập nhật tiến độ và deadline trên một hệ thống client/server.");
        copy.setEditable(false);
        copy.setLineWrap(true);
        copy.setWrapStyleWord(true);
        copy.setOpaque(false);
        copy.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        copy.setForeground(new Color(219, 234, 254));
        copy.setBorder(null);
        gbc.insets = new Insets(18, 0, 0, 0);
        panel.add(copy, gbc);

        gbc.gridy = 3;
        JPanel chips = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.add(chip("Java Swing"));
        chips.add(chip("Socket Client/Server"));
        chips.add(chip("JPA + Hibernate"));
        gbc.insets = new Insets(26, 0, 0, 0);
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

    private static JPanel wrapSouth(JPanel buttons, JLabel hint) {
        JPanel panel = transparentPanel(new BorderLayout(0, 12));
        panel.add(buttons, BorderLayout.CENTER);
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel contentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        return panel;
    }

    private static JPanel toolbarPanel() {
        JPanel panel = cardPanel(new FlowLayout(FlowLayout.LEFT, 10, 8), 12);
        panel.setBorder(new EmptyBorder(8, 10, 8, 10));
        return panel;
    }

    private static JPanel cardPanel(java.awt.LayoutManager layout, int padding) {
        JPanel panel = new RoundedPanel(layout, CARD, BORDER, 22);
        panel.setBackground(CARD);
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

    private static void styleButton(JButton button, Color background) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(background);
        button.setForeground(isLight(background) ? NAVY : Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
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
                new LineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(9, 10, 9, 10)
        ));
    }

    private static void styleArea(JTextArea area) {
        area.setFont(BODY_FONT);
        area.setForeground(NAVY);
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private static void styleFieldIfPossible(Component field) {
        if (field instanceof JTextField textField) {
            styleInput(textField);
        } else if (field instanceof JTextArea textArea) {
            styleArea(textArea);
        } else if (field instanceof JScrollPane scrollPane) {
            scrollPane.setBorder(new LineBorder(new Color(203, 213, 225), 1));
        }
    }

    private boolean confirm(java.awt.Component panel, String title) {
        return JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void message(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private static String profileText(EmployeeView employee) {
        return """
                Mã nhân viên : %s
                Tên đăng nhập: %s
                Họ tên       : %s
                Email       : %s
                Số điện thoại: %s
                Vai trò      : %s
                Trạng thái   : %s
                """.formatted(employee.id(), employee.username(), employee.fullName(), employee.email(),
                employee.phone(), employee.role(), employee.status());
    }

    private static Long parseLong(String text) {
        return Long.parseLong(text.trim());
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim());
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object data) {
        return (List<T>) data;
    }

    private static Map<String, Serializable> map(Object... keyValues) {
        Map<String, Serializable> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], (Serializable) keyValues[i + 1]);
        }
        return map;
    }

    private record ProjectFields(
            JPanel panel,
            JTextField name,
            JTextArea description,
            JTextField startDate,
            JTextField deadline,
            JComboBox<ProjectStatus> status
    ) {
        Map<String, Serializable> toMap(boolean includeStatus) {
            Map<String, Serializable> data = map(
                    "name", name.getText(),
                    "description", description.getText(),
                    "startDate", parseDate(startDate.getText()),
                    "deadline", parseDate(deadline.getText())
            );
            if (includeStatus) {
                data.put("status", (ProjectStatus) status.getSelectedItem());
            }
            return data;
        }

        Map<String, Serializable> toMap(boolean includeStatus, Long projectId) {
            Map<String, Serializable> data = toMap(includeStatus);
            data.put("projectId", projectId);
            return data;
        }
    }

    private record EmployeeOption(Long id, String fullName, String username) {
        @Override
        public String toString() {
            return fullName + " (@" + username + ")";
        }
    }

    private record PriorityOption(TaskPriority priority, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            if (!isSelected) {
                component.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                component.setForeground(new Color(30, 41, 59));
            }
            return component;
        }
    }

    private static class AvatarLabel extends JLabel {
        AvatarLabel(String text) {
            super(text, JLabel.CENTER);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setPreferredSize(new Dimension(42, 42));
            setMinimumSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int diameter = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;
            g2.setColor(BLUE);
            g2.fillOval(x, y, diameter, diameter);
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
                color = isLight(buttonColor) ? new Color(226, 232, 240) : buttonColor.brighter();
            }
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
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
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, radius, radius);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class HeaderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(0, 0, BLUE_DARK, getWidth(), getHeight(), new Color(14, 165, 233));
            g2.setPaint(paint);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.setColor(new Color(255, 255, 255, 35));
            g2.fillOval(getWidth() - 180, -70, 240, 240);
            g2.setColor(new Color(255, 255, 255, 24));
            g2.fillOval(getWidth() - 310, 30, 160, 160);
            g2.dispose();
            super.paintComponent(graphics);
        }

        HeaderPanel() {
            setOpaque(false);
        }
    }

    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(0, 0, NAVY, getWidth(), getHeight(), BLUE_DARK);
            g2.setPaint(paint);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillOval(-90, -90, 280, 280);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillOval(getWidth() - 240, getHeight() - 180, 320, 320);
            g2.dispose();
        }
    }
}
