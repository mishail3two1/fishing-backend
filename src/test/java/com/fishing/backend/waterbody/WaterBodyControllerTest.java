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

    @Test
    void returnsOnlyWaterBodiesInsideBbox() throws Exception {
        jdbcTemplate.update("DELETE FROM water_bodies");
        jdbcTemplate.update("""
                INSERT INTO water_bodies (name, slug, water_type, location, is_published) VALUES
                ('Озеро Смолино', 'smolino', 'LAKE', ST_SetSRID(ST_MakePoint(61.36, 55.13), 4326), true),
                ('Озеро Увильды', 'uvildy', 'LAKE', ST_SetSRID(ST_MakePoint(60.53, 55.52), 4326), true)
                """);

        // область вокруг Челябинска: Смолино внутри, Увильды западнее и севернее
        mockMvc.perform(get("/api/v1/water-bodies").param("bbox", "61.0,54.9,61.8,55.4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Озеро Смолино"));
    }

    @Test
    void rejectsBboxWithMinGreaterThanMax() throws Exception {
        mockMvc.perform(get("/api/v1/water-bodies").param("bbox", "61.8,54.9,61.0,55.4"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("bbox вне допустимых границ или min не меньше max"));
    }

    @Test
    void rejectsBboxWithWrongNumberOfValues() throws Exception {
        mockMvc.perform(get("/api/v1/water-bodies").param("bbox", "61.0,54.9,61.8"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("bbox должен содержать 4 числа: minLon,minLat,maxLon,maxLat"));
    }

    @Test
    void requiresBbox() throws Exception {
        mockMvc.perform(get("/api/v1/water-bodies"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"));
    }

    @Test
    void returnsPublishedCard() throws Exception {
        jdbcTemplate.update("DELETE FROM water_bodies");
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO water_bodies (name, slug, water_type, location, description, district,
                                          access_type, access_notes, has_parking, parking_notes,
                                          rules_notes, data_source, is_published)
                VALUES ('Озеро Смолино', 'smolino', 'LAKE', ST_SetSRID(ST_MakePoint(61.36, 55.13), 4326),
                        'Озеро в черте города', 'Челябинск', 'FREE', 'Бесплатно', true, 'У пляжа',
                        'Весной с лодок нельзя', 'telegram: секретный канал', true)
                RETURNING id
                """, Long.class);

        mockMvc.perform(get("/api/v1/water-bodies/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Озеро Смолино"))
                .andExpect(jsonPath("$.lat").value(55.13))
                .andExpect(jsonPath("$.lon").value(61.36))
                .andExpect(jsonPath("$.accessType").value("FREE"))
                .andExpect(jsonPath("$.accessNotes").value("Бесплатно"))
                .andExpect(jsonPath("$.hasParking").value(true))
                .andExpect(jsonPath("$.parkingNotes").value("У пляжа"))
                .andExpect(jsonPath("$.rulesNotes").value("Весной с лодок нельзя"))
                // источник данных видят только админы
                .andExpect(jsonPath("$.dataSource").doesNotExist());
    }

    @Test
    void hidesDraftCard() throws Exception {
        jdbcTemplate.update("DELETE FROM water_bodies");
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO water_bodies (name, slug, water_type, location, is_published)
                VALUES ('Черновик', 'draft', 'POND', ST_SetSRID(ST_MakePoint(61.0, 55.0), 4326), false)
                RETURNING id
                """, Long.class);

        mockMvc.perform(get("/api/v1/water-bodies/" + id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Водоём не найден"));
    }

    @Test
    void returns404ForUnknownId() throws Exception {
        jdbcTemplate.update("DELETE FROM water_bodies");

        mockMvc.perform(get("/api/v1/water-bodies/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"));
    }

    @Test
    void rejectsNonNumericId() throws Exception {
        mockMvc.perform(get("/api/v1/water-bodies/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"));
    }
}
