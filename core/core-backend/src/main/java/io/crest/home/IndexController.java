package io.crest.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
// 定义当前业务对象的核心职责
public class IndexController {

    private static final String INDEX_PAGE = "index.html";
    private static final String PANEL_PAGE = "panel.html";

    @GetMapping("/")
    // 执行当前服务方法并返回处理结果
    public String index() {
        return INDEX_PAGE;
    }

    @GetMapping("/panel")
    // 执行当前服务方法并返回处理结果
    public String panel() {
        return PANEL_PAGE;
    }

    @GetMapping({"/doc.html", "/swagger-ui.html", "/swagger-ui/index.html"})
    // 执行当前服务方法并返回处理结果
    public String doc() {
        return "redirect:/webjars/swagger-ui/index.html";
    }

}
