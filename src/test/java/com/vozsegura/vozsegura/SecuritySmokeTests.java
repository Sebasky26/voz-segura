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
    void publicDenunciaIsAccessible() throws Exception {
        mockMvc.perform(get("/denuncia"))
                .andExpect(status().isOk());
    }

    @Test
    void staffCasosRequiresAuth() throws Exception {
        mockMvc.perform(get("/staff/casos"))
                .andExpect(status().is3xxRedirection());
    }
}
