package com.vozsegura.vozsegura.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping
    public String panel() {
        return "admin/panel";
    }

    @GetMapping("/reglas")
    public String reglas() {
        return "admin/reglas";
    }

    @GetMapping("/logs")
    public String logs() {
        return "admin/logs";
    }

    @GetMapping("/revelacion")
    public String revelacion() {
        return "admin/revelacion";
    }
}
