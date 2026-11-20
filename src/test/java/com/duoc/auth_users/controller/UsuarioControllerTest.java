package com.duoc.auth_users.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private int crearUsuario(String adminToken, String email) throws Exception {
        String body = mockMvc.perform(post("/usuarios").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Ana","apellido":"Perez","email":"%s","password":"Clave2026!"}
                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void usuarioEditaSuPerfilSinEnviarPasswordYLaConserva() throws Exception {
        String admin = login("admin@test.cl", "Admin2026!");
        int id = crearUsuario(admin, "perfil@test.cl");
        String usuario = login("perfil@test.cl", "Clave2026!");

        mockMvc.perform(put("/usuarios/" + id).header("Authorization", usuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Maria\",\"apellido\":\"Perez\",\"email\":\"perfil@test.cl\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Maria"));

        login("perfil@test.cl", "Clave2026!");
    }

    @Test
    void editarConPasswordDebilDevuelve400() throws Exception {
        String admin = login("admin@test.cl", "Admin2026!");
        int id = crearUsuario(admin, "debil@test.cl");

        mockMvc.perform(put("/usuarios/" + id).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"email\":\"debil@test.cl\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminNoPuedeCrearUsuarioSinPassword() throws Exception {
        String admin = login("admin@test.cl", "Admin2026!");

        mockMvc.perform(post("/usuarios").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"email\":\"sinpass@test.cl\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioComunNoPuedeListarUsuariosYSinTokenEs401() throws Exception {
        String admin = login("admin@test.cl", "Admin2026!");
        crearUsuario(admin, "comun@test.cl");
        String usuario = login("comun@test.cl", "Clave2026!");

        mockMvc.perform(get("/usuarios").header("Authorization", usuario)).andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/usuarios").header("Authorization", admin)).andExpect(status().isOk());
    }
}
