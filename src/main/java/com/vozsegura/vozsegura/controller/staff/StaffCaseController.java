package com.vozsegura.vozsegura.controller.staff;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffCaseController {

    @GetMapping("/casos")
    public String listCases() {
        return "staff/casos-list";
    }

    @GetMapping("/casos/{trackingId}")
    public String viewCase(@PathVariable String trackingId) {
        return "staff/caso-detalle";
    }

    @PostMapping("/casos/{trackingId}/estado")
    public String updateEstado(@PathVariable String trackingId) {
        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/derivar")
    public String derivar(@PathVariable String trackingId) {
        return "redirect:/staff/casos/" + trackingId;
    }
}
