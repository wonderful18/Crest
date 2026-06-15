package io.crest.substitute.permissions.user.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
// 定义当前业务对象的核心职责
public class UserImportRow {

    @ExcelProperty("账号")
    private String account;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("电话")
    private String phone;
}
