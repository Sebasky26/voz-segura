package com.vozsegura.vozsegura;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecuritySmokeTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void publicDenunciaRequiresAuth() throws Exception {
        // Con ZTA, /denuncia redirige a /auth/login si no está autenticado
        mockMvc.perform(get("/denuncia"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void authLoginIsAccessible() throws Exception {
        // La página de login debe ser accesible públicamente
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void staffCasosRequiresAuth() throws Exception {
        mockMvc.perform(get("/staff/casos"))
                .andExpect(status().is3xxRedirection());
    }
}
