package com.duoc.auth_users.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String registro(String email, String password) {
        return """
                { "nombre": "Ana", "apellido": "Perez", "email": "%s", "password": "%s" }
                """.formatted(email, password);
    }

    @Test
    void registroConPasswordDebilDevuelve400ConUnDetallePorRegla() throws Exception {
        mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registro("debil@test.cl", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(4))
                .andExpect(jsonPath("$.detalles", hasItem("password: La contraseña debe incluir al menos un numero")));
    }

    @Test
    void registroConPasswordSeguraDevuelve201() throws Exception {
        mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registro("segura@test.cl", "Segura2026!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("USUARIO"));
    }
}
