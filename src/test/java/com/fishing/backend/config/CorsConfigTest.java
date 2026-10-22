package com.fishing.backend.config;

import com.fishing.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Имитирует preflight-запрос браузера: свой фронтенд пропускаем, чужой сайт получает отказ.
@SpringBootTest(properties = "app.cors.allowed-origin=http://localhost:5173")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CorsConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/water-bodies")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void rejectsForeignOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/water-bodies")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}