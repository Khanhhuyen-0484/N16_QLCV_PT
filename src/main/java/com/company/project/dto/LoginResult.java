package com.company.project.dto;

import java.io.Serializable;

/** Kết quả đăng nhập server trả về cho client: token phiên + thông tin tài khoản. */
public record LoginResult(String token, EmployeeView employee) implements Serializable {
}
