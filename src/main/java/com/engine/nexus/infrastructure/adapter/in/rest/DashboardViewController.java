package com.engine.nexus.infrastructure.adapter.in.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/dashboard/index.html";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard/index.html";
    }
}
