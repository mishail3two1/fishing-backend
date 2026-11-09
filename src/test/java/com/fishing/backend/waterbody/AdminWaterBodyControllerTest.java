package com.fishing.backend.waterbody;

import com.fishing.backend.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Админка водоёмов: доступ только ADMIN, создание черновика, публикация, конфликт slug, валидация.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminWaterBodyControllerTest {

    private static final String ADMIN_URL = "/api/v1/admin/water-bodies";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM water_bodies");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(post(ADMIN_URL).contentType(MediaType.APPLICATION_JSON).content(body("smolino", false)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUserWithoutAdminRole() throws Exception {
        String userToken = tokenFor("user@example.com", "USER");

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("smolino", false)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Недостаточно прав"));
    }

    @Test
    void createsDraftAndPublishesIt() throws Exception {
        String adminToken = tokenFor("admin@example.com", "ADMIN");

        MvcResult created = mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("smolino", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("smolino"))
                .andExpect(jsonPath("$.isPublished").value(false))
                .andExpect(jsonPath("$.dataSource").value("форум рыбаков"))
                // координаты не перепутаны: в JTS x = долгота, y = широта
                .andExpect(jsonPath("$.lat").value(55.13))
                .andExpect(jsonPath("$.lon").value(61.36))
                .andExpect(jsonPath("$.verifiedAt").isNotEmpty())
                .andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // черновик для пользователей не существует
        mockMvc.perform(get("/api/v1/water-bodies/" + id))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(ADMIN_URL + "/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("smolino", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublished").value(true));

        // после публикации водоём виден на публичной карточке, без токена
        mockMvc.perform(get("/api/v1/water-bodies/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Озеро Смолино"))
                .andExpect(jsonPath("$.dataSource").doesNotExist());
    }

    @Test
    void rejectsDuplicateSlugOnCreate() throws Exception {
        String adminToken = tokenFor("admin@example.com", "ADMIN");
        create(adminToken, "smolino");

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("smolino", false)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Водоём с таким slug уже есть"));
    }

    @Test
    void updateKeepsOwnSlugButRejectsSlugOfAnotherWaterBody() throws Exception {
        String adminToken = tokenFor("admin@example.com", "ADMIN");
        Number smolinoId = create(adminToken, "smolino");
        Number uvildyId = create(adminToken, "uvildy");

        // свой же slug — не конфликт
        mockMvc.perform(put(ADMIN_URL + "/" + smolinoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("smolino", true)))
                .andExpect(status().isOk());

        // slug другого водоёма — конфликт
        mockMvc.perform(put(ADMIN_URL + "/" + uvildyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("smolino", false)))
                .andExpect(status().isConflict());
    }

    @Test
    void returns404WhenUpdatingUnknownWaterBody() throws Exception {
        String adminToken = tokenFor("admin@example.com", "ADMIN");

        mockMvc.perform(put(ADMIN_URL + "/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("smolino", false)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidData() throws Exception {
        String adminToken = tokenFor("admin@example.com", "ADMIN");

        // заглавные буквы и пробел в slug
        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Smolino Lake", false)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"));

        // широта за пределами -90..90
        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("smolino", false).replace("55.13", "120")))
                .andExpect(status().isBadRequest());
    }

    // регистрирует пользователя, выдаёт ему роль и входит: роль попадает в токен при входе
    private String tokenFor(String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"" + email + "\", \"password\": \"secret123\", \"displayName\": \"Тест\" }"))
                .andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE users SET role = ? WHERE email = ?", role, email);

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"" + email + "\", \"password\": \"secret123\" }"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private Number create(String adminToken, String slug) throws Exception {
        MvcResult result = mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(slug, false)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String body(String slug, boolean published) {
        return """
                {
                  "name": "Озеро Смолино", "slug": "%s", "waterType": "LAKE",
                  "lat": 55.13, "lon": 61.36, "accessType": "FREE",
                  "dataSource": "форум рыбаков", "isPublished": %s
                }
                """.formatted(slug, published);
    }
}
