package com.vozsegura.vozsegura.controller.publicview;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TermsController {

    @GetMapping("/terms")
    public String terms() {
        return "public/terms";
    }
}
