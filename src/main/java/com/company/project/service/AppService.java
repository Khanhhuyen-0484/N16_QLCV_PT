package com.company.project.service;

import com.company.project.dto.DashboardStats;
import com.company.project.dto.EmployeeDirectoryView;
import com.company.project.dto.EmployeeView;
import com.company.project.dto.ProjectView;
import com.company.project.dto.TaskView;
import com.company.project.entity.AccountStatus;
import com.company.project.entity.Employee;
import com.company.project.entity.Project;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.ProjectTask;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.entity.TaskStatus;
import com.company.project.util.JpaUtil;
import com.company.project.util.PasswordUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AppService {
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public void seedDefaultManager() {
        EntityManager em = JpaUtil.em();
        try {
            long count = em.createQuery("select count(e) from Employee e", Long.class).getSingleResult();
            if (count > 0) {
                long projectCount = em.createQuery("select count(p) from Project p", Long.class).getSingleResult();
                if (projectCount == 0) {
                    em.getTransaction().begin();
                    Project project = new Project();
                    project.setName("Dự án công ty");
                    project.setDescription("Dự án duy nhất dùng để quản lý phân công công việc trong công ty.");
                    project.setStartDate(LocalDate.now());
                    project.setDeadline(LocalDate.now().plusMonths(3));
                    project.setStatus(ProjectStatus.IN_PROGRESS);
                    em.persist(project);
                    em.getTransaction().commit();
                }
                return;
            }
            em.getTransaction().begin();
            Employee manager = new Employee();
            manager.setUsername("admin");
            manager.setPasswordHash(PasswordUtil.hash("admin123"));
            manager.setFullName("Default Manager");
            manager.setEmail("admin@company.local");
            manager.setPhone("0000000000");
            manager.setRole(Role.MANAGER);
            manager.setStatus(AccountStatus.ACTIVE);
            em.persist(manager);
            Project project = new Project();
            project.setName("Dự án công ty");
            project.setDescription("Dự án duy nhất dùng để quản lý phân công công việc trong công ty.");
            project.setStartDate(LocalDate.now());
            project.setDeadline(LocalDate.now().plusMonths(3));
            project.setStatus(ProjectStatus.IN_PROGRESS);
            project.getMembers().add(manager);
            em.persist(project);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void seedDemoData() {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Employee manager = findByUsername(em, "admin");
            if (manager == null) {
                manager = new Employee();
                manager.setUsername("admin");
                manager.setPasswordHash(PasswordUtil.hash("admin123"));
                manager.setFullName("Default Manager");
                manager.setEmail("admin@company.local");
                manager.setPhone("0000000000");
                manager.setRole(Role.MANAGER);
                manager.setStatus(AccountStatus.ACTIVE);
                em.persist(manager);
            }

            Project project = primaryProject(em);
            project.setName("Dự án công ty");
            project.setDescription("Dự án quản lý phân công công việc trong một dự án của công ty.");
            if (project.getStartDate() == null) {
                project.setStartDate(LocalDate.now());
            }
            if (project.getDeadline() == null) {
                project.setDeadline(LocalDate.now().plusMonths(3));
            }
            project.setStatus(ProjectStatus.IN_PROGRESS);
            project.getMembers().add(manager);

            Employee an = demoEmployee(em, "nhanvien01", "Nguyễn Minh An",
                    "an@company.local", "0901000001", AccountStatus.ACTIVE);
            Employee minh = demoEmployee(em, "minhdev", "Nguyễn Văn Minh",
                    "minh@company.local", "0912345678", AccountStatus.ACTIVE);
            Employee pending = demoEmployee(em, "nhanvien_moi", "Nhân Viên Mới",
                    "new.employee@company.local", "0987654321", AccountStatus.PENDING);
            project.getMembers().add(an);
            project.getMembers().add(minh);
            project.getMembers().add(pending);

            long taskCount = em.createQuery("select count(t) from ProjectTask t", Long.class).getSingleResult();
            if (taskCount == 0) {
                em.persist(demoTask(project, manager, an, "Thiết kế giao diện Dashboard",
                        "Hoàn thiện giao diện tổng quan dự án, thống kê và danh sách công việc gần deadline.",
                        LocalDate.now().minusDays(5), LocalDate.now().plusDays(3), 65,
                        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "Đang hoàn thiện bố cục và màu sắc."));
                em.persist(demoTask(project, manager, minh, "Xây dựng chức năng phân công công việc",
                        "Tạo form giao việc, chọn nhân viên, deadline và mức độ ưu tiên.",
                        LocalDate.now().minusDays(4), LocalDate.now().plusDays(7), 35,
                        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, "Đã nối socket client/server."));
                em.persist(demoTask(project, manager, an, "Kiểm thử đăng nhập và phân quyền",
                        "Kiểm tra luồng đăng nhập, đổi mật khẩu, duyệt tài khoản và khóa tài khoản.",
                        LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), 100,
                        TaskStatus.COMPLETED, TaskPriority.MEDIUM, "Đã hoàn thành kiểm thử chính."));
                em.persist(demoTask(project, manager, minh, "Viết báo cáo tiến độ dự án",
                        "Tổng hợp số lượng công việc, tiến độ và trạng thái quá hạn.",
                        LocalDate.now().minusDays(7), LocalDate.now().minusDays(2), 20,
                        TaskStatus.IN_PROGRESS, TaskPriority.URGENT, "Cần cập nhật phần công việc quá hạn."));
                em.persist(demoTask(project, manager, an, "Chuẩn bị dữ liệu demo",
                        "Tạo dữ liệu mẫu để máy khác clone project có thể chạy thử ngay.",
                        LocalDate.now(), LocalDate.now().plusDays(10), 0,
                        TaskStatus.TODO, TaskPriority.LOW, ""));
            }
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public LoginResult login(String username, String password) {
        EntityManager em = JpaUtil.em();
        try {
            Employee employee = findByUsername(em, username);
            if (employee == null || !PasswordUtil.matches(password, employee.getPasswordHash())) {
                throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
            }
            if (employee.getStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Tài khoản chưa được kích hoạt hoặc đã bị khóa.");
            }
            String token = UUID.randomUUID().toString();
            sessions.put(token, employee.getId());
            return new LoginResult(token, EmployeeView.from(employee));
        } finally {
            em.close();
        }
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    public EmployeeView register(String username, String password, String fullName, String email, String phone) {
        EntityManager em = JpaUtil.em();
        try {
            if (findByUsername(em, username) != null) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
            }
            em.getTransaction().begin();
            Employee employee = new Employee();
            employee.setUsername(username);
            employee.setPasswordHash(PasswordUtil.hash(password));
            employee.setFullName(fullName);
            employee.setEmail(email);
            employee.setPhone(phone);
            employee.setRole(Role.EMPLOYEE);
            employee.setStatus(AccountStatus.PENDING);
            em.persist(employee);
            em.getTransaction().commit();
            return EmployeeView.from(employee);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public EmployeeView me(String token) {
        EntityManager em = JpaUtil.em();
        try {
            return EmployeeView.from(currentUser(em, token));
        } finally {
            em.close();
        }
    }

    public EmployeeView updateProfile(String token, String fullName, String email, String phone) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Employee employee = currentUser(em, token);
            employee.setFullName(fullName);
            employee.setEmail(email);
            employee.setPhone(phone);
            em.getTransaction().commit();
            return EmployeeView.from(employee);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void changePassword(String token, String oldPassword, String newPassword) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Employee employee = currentUser(em, token);
            if (!PasswordUtil.matches(oldPassword, employee.getPasswordHash())) {
                throw new IllegalArgumentException("Mật khẩu cũ không đúng.");
            }
            employee.setPasswordHash(PasswordUtil.hash(newPassword));
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public List<TaskView> myTasks(String token) {
        EntityManager em = JpaUtil.em();
        try {
            Employee employee = currentUser(em, token);
            return em.createQuery("""
                    select t from ProjectTask t
                    join fetch t.project
                    join fetch t.assignee
                    where t.assignee.id = :employeeId
                    order by t.deadline asc nulls last, t.id desc
                    """, ProjectTask.class)
                    .setParameter("employeeId", employee.getId())
                    .getResultList()
                    .stream()
                    .map(TaskView::from)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } finally {
            em.close();
        }
    }

    public TaskView updateTaskProgress(String token, long taskId, int progress) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Employee employee = currentUser(em, token);
            ProjectTask task = taskForAssignee(em, taskId, employee.getId());
            task.setProgress(progress);
            task.setStatus(progress >= 100 ? TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS);
            task.setCompletedAt(progress >= 100 ? LocalDateTime.now() : null);
            em.getTransaction().commit();
            return TaskView.from(task);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public TaskView completeTask(String token, long taskId) {
        return updateTaskProgress(token, taskId, 100);
    }

    public TaskView addTaskNote(String token, long taskId, String note) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Employee employee = currentUser(em, token);
            ProjectTask task = taskForAssignee(em, taskId, employee.getId());
            task.setNote(note);
            if (task.getStatus() == TaskStatus.TODO) {
                task.setStatus(TaskStatus.IN_PROGRESS);
            }
            em.getTransaction().commit();
            return TaskView.from(task);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public TaskView submitTaskResult(String token, long taskId, String resultText, String resultLink, String note) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Employee employee = currentUser(em, token);
            ProjectTask task = taskForAssignee(em, taskId, employee.getId());
            task.setResultText(resultText);
            task.setResultLink(resultLink);
            task.setNote(note);
            task.setProgress(100);
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            em.getTransaction().commit();
            return TaskView.from(task);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public List<EmployeeView> listEmployees(String token) {
        EntityManager em = JpaUtil.em();
        try {
            requireManager(em, token);
            return em.createQuery("select e from Employee e order by e.id", Employee.class)
                    .getResultList()
                    .stream()
                    .map(EmployeeView::from)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } finally {
            em.close();
        }
    }

    public List<EmployeeDirectoryView> listEmployeeDirectory(String token) {
        EntityManager em = JpaUtil.em();
        try {
            requireManager(em, token);
            return em.createQuery("""
                    select e from Employee e
                    where e.role = :role
                    order by e.fullName, e.id
                    """, Employee.class)
                    .setParameter("role", Role.EMPLOYEE)
                    .getResultList()
                    .stream()
                    .map(employee -> new EmployeeDirectoryView(
                            EmployeeView.from(employee),
                            countTasksForEmployee(em, employee.getId(), TaskStatus.IN_PROGRESS),
                            countTasksForEmployee(em, employee.getId(), TaskStatus.COMPLETED)
                                    + countTasksForEmployee(em, employee.getId(), TaskStatus.DONE)
                    ))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } finally {
            em.close();
        }
    }

    public EmployeeView createEmployee(String token, String username, String password, String fullName, String email, String phone) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            requireManager(em, token);
            if (findByUsername(em, username) != null) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
            }
            Employee employee = new Employee();
            employee.setUsername(username);
            employee.setPasswordHash(PasswordUtil.hash(password));
            employee.setFullName(fullName);
            employee.setEmail(email);
            employee.setPhone(phone);
            employee.setRole(Role.EMPLOYEE);
            employee.setStatus(AccountStatus.ACTIVE);
            em.persist(employee);
            primaryProject(em).getMembers().add(employee);
            em.getTransaction().commit();
            return EmployeeView.from(employee);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public EmployeeView updateEmployee(String token, long employeeId, String fullName, String email, String phone, Role role) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            requireManager(em, token);
            Employee employee = employeeById(em, employeeId);
            employee.setFullName(fullName);
            employee.setEmail(email);
            employee.setPhone(phone);
            employee.setRole(role == null ? Role.EMPLOYEE : role);
            em.getTransaction().commit();
            return EmployeeView.from(employee);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public List<EmployeeView> listPendingEmployees(String token) {
        EntityManager em = JpaUtil.em();
        try {
            requireManager(em, token);
            return em.createQuery("select e from Employee e where e.status = :status order by e.id", Employee.class)
                    .setParameter("status", AccountStatus.PENDING)
                    .getResultList()
                    .stream()
                    .map(EmployeeView::from)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } finally {
            em.close();
        }
    }

    public EmployeeView approveEmployee(String token, long employeeId) {
        return changeEmployeeStatus(token, employeeId, AccountStatus.ACTIVE);
    }

    public EmployeeView disableEmployee(String token, long employeeId) {
        Long currentId = sessions.get(token);
        if (currentId != null && currentId == employeeId) {
            throw new IllegalArgumentException("Không thể khóa chính tài khoản đang đăng nhập.");
        }
        return changeEmployeeStatus(token, employeeId, AccountStatus.DISABLED);
    }

    public EmployeeView enableEmployee(String token, long employeeId) {
        return changeEmployeeStatus(token, employeeId, AccountStatus.ACTIVE);
    }

    public ProjectView createProject(String token, String name, String description, LocalDate startDate, LocalDate deadline) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            requireManager(em, token);
            Project project = primaryProject(em);
            project.setName(name);
            project.setDescription(description);
            project.setStartDate(startDate);
            project.setDeadline(deadline);
            project.setStatus(ProjectStatus.IN_PROGRESS);
            em.getTransaction().commit();
            return ProjectView.from(project);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public List<ProjectView> listProjects(String token) {
        EntityManager em = JpaUtil.em();
        try {
            Employee current = currentUser(em, token);
            List<Project> projects;
            if (current.getRole() == Role.MANAGER) {
                Project project = primaryProject(em);
                project.getMembers().size();
                projects = List.of(project);
            } else {
                projects = em.createQuery("""
                        select distinct p from Project p
                        left join fetch p.members m
                        where m.id = :employeeId
                        order by p.id
                        """, Project.class)
                        .setParameter("employeeId", current.getId())
                        .getResultList();
                if (projects.isEmpty()) {
                    Project project = primaryProject(em);
                    project.getMembers().size();
                    projects = List.of(project);
                }
            }
            return projects.stream()
                    .map(ProjectView::from)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } finally {
            em.close();
        }
    }

    public ProjectView updateProject(String token, long projectId, String name, String description,
                                     LocalDate startDate, LocalDate deadline, ProjectStatus status) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            requireManager(em, token);
            Project project = primaryProject(em);
            project.setName(name);
            project.setDescription(description);
            project.setStartDate(startDate);
            project.setDeadline(deadline);
            project.setStatus(status);
            project.getMembers().size();
            em.getTransaction().commit();
            return ProjectView.from(project);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void deleteProject(String token, long projectId) {
        EntityManager em = JpaUtil.em();
        try {
            requireManager(em, token);
            throw new IllegalArgumentException("Hệ thống chỉ quản lý một dự án duy nhất, không cho phép xóa dự án.");
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public ProjectView assignProjectMember(String token, long projectId, long employeeId) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            requireManager(em, token);
            Project project = primaryProject(em);
            Employee employee = employeeById(em, employeeId);
            if (employee.getStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Nhân viên chưa được kích hoạt.");
            }
            project.getMembers().add(employee);
            project.getMembers().size();
            em.getTransaction().commit();
            return ProjectView.from(project);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public TaskView createTask(String token, long projectId, long assigneeId, String title, String description,
                               LocalDate startDate, LocalDate deadline) {
        return createTask(token, projectId, assigneeId, title, description, startDate, deadline, TaskPriority.MEDIUM);
    }

    public TaskView createTask(String token, long projectId, long assigneeId, String title, String description,
                               LocalDate startDate, LocalDate deadline, TaskPriority priority) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            Employee assigner = requireManager(em, token);
            Project project = primaryProject(em);
            Employee assignee = employeeById(em, assigneeId);
            if (assignee.getStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Nhân viên chưa được kích hoạt.");
            }
            project.getMembers().add(assignee);
            ProjectTask task = new ProjectTask();
            task.setProject(project);
            task.setAssignee(assignee);
            task.setTitle(title);
            task.setDescription(description);
            task.setStartDate(startDate);
            task.setDeadline(deadline);
            task.setProgress(0);
            task.setStatus(TaskStatus.TODO);
            task.setPriority(priority);
            task.setAssigner(assigner);
            em.persist(task);
            em.getTransaction().commit();
            return TaskView.from(task);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public List<TaskView> listTasks(String token) {
        EntityManager em = JpaUtil.em();
        try {
            requireManager(em, token);
            return em.createQuery("""
                    select t from ProjectTask t
                    join fetch t.project
                    join fetch t.assignee
                    order by t.project.id desc, t.id desc
                    """, ProjectTask.class)
                    .getResultList()
                    .stream()
                    .map(TaskView::from)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } finally {
            em.close();
        }
    }

    public DashboardStats dashboard(String token) {
        EntityManager em = JpaUtil.em();
        try {
            Employee current = currentUser(em, token);
            boolean manager = current.getRole() == Role.MANAGER;
            String assigneeFilter = manager ? "" : " and t.assignee.id = :employeeId";
            long totalEmployees = manager
                    ? em.createQuery("select count(e) from Employee e where e.role = :role and e.status = :status", Long.class)
                    .setParameter("role", Role.EMPLOYEE)
                    .setParameter("status", AccountStatus.ACTIVE)
                    .getSingleResult()
                    : 1;
            long totalTasks = countTasks(em, assigneeFilter, current.getId(), null, false);
            long todo = countTasks(em, assigneeFilter, current.getId(), TaskStatus.TODO, false);
            long inProgress = countTasks(em, assigneeFilter, current.getId(), TaskStatus.IN_PROGRESS, false);
            long completed = countTasks(em, assigneeFilter, current.getId(), TaskStatus.COMPLETED, false)
                    + countTasks(em, assigneeFilter, current.getId(), TaskStatus.DONE, false);
            long overdue = countTasks(em, assigneeFilter, current.getId(), null, true);
            int progress = totalTasks == 0 ? 0 : (int) Math.round((completed * 100.0) / totalTasks);
            String nearQuery = """
                    select t from ProjectTask t
                    join fetch t.project
                    join fetch t.assignee
                    left join fetch t.assigner
                    where t.deadline is not null
                      and t.status <> :completed
                      and t.status <> :done
                    """ + (manager ? "" : " and t.assignee.id = :employeeId") + " order by t.deadline asc";
            var query = em.createQuery(nearQuery, ProjectTask.class)
                    .setParameter("completed", TaskStatus.COMPLETED)
                    .setParameter("done", TaskStatus.DONE)
                    .setMaxResults(5);
            if (!manager) {
                query.setParameter("employeeId", current.getId());
            }
            List<TaskView> near = query.getResultList().stream().map(TaskView::from)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            return new DashboardStats(totalEmployees, totalTasks, todo, inProgress, completed, overdue, progress, near);
        } finally {
            em.close();
        }
    }

    private EmployeeView changeEmployeeStatus(String token, long employeeId, AccountStatus status) {
        EntityManager em = JpaUtil.em();
        try {
            em.getTransaction().begin();
            requireManager(em, token);
            Employee employee = employeeById(em, employeeId);
            employee.setStatus(status);
            em.getTransaction().commit();
            return EmployeeView.from(employee);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    private Employee currentUser(EntityManager em, String token) {
        Long id = sessions.get(token);
        if (id == null) {
            throw new SecurityException("Phiên đăng nhập không hợp lệ.");
        }
        Employee employee = em.find(Employee.class, id);
        if (employee == null || employee.getStatus() != AccountStatus.ACTIVE) {
            throw new SecurityException("Tài khoản không khả dụng.");
        }
        return employee;
    }

    private Employee requireManager(EntityManager em, String token) {
        Employee employee = currentUser(em, token);
        if (employee.getRole() != Role.MANAGER) {
            throw new SecurityException("Chức năng chỉ dành cho quản lý.");
        }
        return employee;
    }

    private Employee findByUsername(EntityManager em, String username) {
        try {
            TypedQuery<Employee> query = em.createQuery("select e from Employee e where e.username = :username", Employee.class);
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private Employee demoEmployee(EntityManager em, String username, String fullName, String email,
                                  String phone, AccountStatus status) {
        Employee employee = findByUsername(em, username);
        if (employee != null) {
            return employee;
        }
        employee = new Employee();
        employee.setUsername(username);
        employee.setPasswordHash(PasswordUtil.hash("123456"));
        employee.setFullName(fullName);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setRole(Role.EMPLOYEE);
        employee.setStatus(status);
        em.persist(employee);
        return employee;
    }

    private ProjectTask demoTask(Project project, Employee assigner, Employee assignee, String title,
                                 String description, LocalDate startDate, LocalDate deadline, int progress,
                                 TaskStatus status, TaskPriority priority, String note) {
        ProjectTask task = new ProjectTask();
        task.setProject(project);
        task.setAssigner(assigner);
        task.setAssignee(assignee);
        task.setTitle(title);
        task.setDescription(description);
        task.setStartDate(startDate);
        task.setDeadline(deadline);
        task.setProgress(progress);
        task.setStatus(status);
        task.setPriority(priority);
        task.setNote(note);
        if (status == TaskStatus.COMPLETED || status == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now().minusDays(1));
        }
        return task;
    }

    private Employee employeeById(EntityManager em, long employeeId) {
        Employee employee = em.find(Employee.class, employeeId);
        if (employee == null) {
            throw new IllegalArgumentException("Không tìm thấy nhân viên.");
        }
        return employee;
    }

    private Project projectById(EntityManager em, long projectId) {
        Project project = em.find(Project.class, projectId);
        if (project == null) {
            throw new IllegalArgumentException("Không tìm thấy dự án.");
        }
        return project;
    }

    private Project primaryProject(EntityManager em) {
        List<Project> projects = em.createQuery("select distinct p from Project p left join fetch p.members order by p.id", Project.class)
                .setMaxResults(1)
                .getResultList();
        if (!projects.isEmpty()) {
            return projects.getFirst();
        }
        boolean startedHere = false;
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
            startedHere = true;
        }
        Project project = new Project();
        project.setName("Dự án công ty");
        project.setDescription("Dự án duy nhất dùng để quản lý phân công công việc trong công ty.");
        project.setStartDate(LocalDate.now());
        project.setDeadline(LocalDate.now().plusMonths(3));
        project.setStatus(ProjectStatus.IN_PROGRESS);
        em.persist(project);
        if (startedHere) {
            em.getTransaction().commit();
        }
        return project;
    }

    private long countTasks(EntityManager em, String assigneeFilter, long employeeId, TaskStatus status, boolean overdue) {
        String jpql = "select count(t) from ProjectTask t where 1=1" + assigneeFilter;
        if (status != null) {
            jpql += " and t.status = :status";
        }
        if (overdue) {
            jpql += " and t.deadline < :today and t.status <> :completed and t.status <> :done";
        }
        var query = em.createQuery(jpql, Long.class);
        if (!assigneeFilter.isBlank()) {
            query.setParameter("employeeId", employeeId);
        }
        if (status != null) {
            query.setParameter("status", status);
        }
        if (overdue) {
            query.setParameter("today", LocalDate.now());
            query.setParameter("completed", TaskStatus.COMPLETED);
            query.setParameter("done", TaskStatus.DONE);
        }
        return query.getSingleResult();
    }

    private long countTasksForEmployee(EntityManager em, long employeeId, TaskStatus status) {
        return em.createQuery("""
                select count(t) from ProjectTask t
                where t.assignee.id = :employeeId and t.status = :status
                """, Long.class)
                .setParameter("employeeId", employeeId)
                .setParameter("status", status)
                .getSingleResult();
    }

    private ProjectTask taskForAssignee(EntityManager em, long taskId, long assigneeId) {
        ProjectTask task = em.createQuery("""
                select t from ProjectTask t
                join fetch t.project
                join fetch t.assignee
                where t.id = :taskId and t.assignee.id = :assigneeId
                """, ProjectTask.class)
                .setParameter("taskId", taskId)
                .setParameter("assigneeId", assigneeId)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công việc được giao."));
        return task;
    }

    public record LoginResult(String token, EmployeeView employee) implements java.io.Serializable {
    }
}
