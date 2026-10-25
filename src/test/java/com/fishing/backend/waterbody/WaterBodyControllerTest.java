package com.fishing.backend.waterbody;

import com.fishing.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Проверяет, что карта получает только опубликованные водоёмы и координаты не перепутаны.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WaterBodyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void returnsOnlyPublishedWaterBodies() throws Exception {
        // тестовые данные кладём прямо в базу: так тест не зависит от того, что в ней было раньше
        jdbcTemplate.update("DELETE FROM water_bodies");
        jdbcTemplate.update("""
                INSERT INTO water_bodies (name, slug, water_type, location, is_published) VALUES
                ('Озеро Смолино', 'smolino', 'LAKE', ST_SetSRID(ST_MakePoint(61.36, 55.13), 4326), true),
                ('Черновик', 'draft', 'POND', ST_SetSRID(ST_MakePoint(61.0, 55.0), 4326), false)
                """);

        mockMvc.perform(get("/api/v1/water-bodies").param("bbox", "61.0,54.9,61.8,55.4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Озеро Смолино"))
                .andExpect(jsonPath("$[0].waterType").value("LAKE"))
                .andExpect(jsonPath("$[0].lat").value(55.13))
                .andExpect(jsonPath("$[0].lon").value(61.36));
    }
}
