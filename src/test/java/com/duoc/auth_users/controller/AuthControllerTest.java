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

import com.jayway.jsonpath.JsonPath;

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

    private void login(String email, String password, int statusEsperado) throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().is(statusEsperado));
    }

    private void restablecer(String email, String codigo, String password, int statusEsperado) throws Exception {
        mockMvc.perform(post("/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"codigo\":\"%s\",\"nuevaPassword\":\"%s\"}"
                        .formatted(email, codigo, password)))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    void flujoCompletoDeRecuperacionDeContraseña() throws Exception {
        mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registro("olvido@test.cl", "Antigua2026!")))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"olvido@test.cl\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoDemo").isString())
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(body, "$.codigoDemo");

        // Contraseña debil -> 400, y el codigo sigue sirviendo
        restablecer("olvido@test.cl", codigo, "debil", 400);
        restablecer("olvido@test.cl", codigo, "Nueva2026!", 204);

        login("olvido@test.cl", "Nueva2026!", 200);
        login("olvido@test.cl", "Antigua2026!", 401);

        // Un solo uso
        restablecer("olvido@test.cl", codigo, "Otra2026!", 400);
    }

    @Test
    void recuperarConEmailInexistenteDevuelve200SinCodigo() throws Exception {
        mockMvc.perform(post("/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nadie@test.cl\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoDemo").doesNotExist());
    }

    @Test
    void restablecerConCodigoMalFormadoDevuelve400() throws Exception {
        restablecer("admin@test.cl", "12ab", "Nueva2026!", 400);
    }
}
