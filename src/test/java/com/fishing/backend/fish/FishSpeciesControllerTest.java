package com.fishing.backend.fish;

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

// Справочник рыб: открыт без входа, отсортирован по русскому названию, лишние поля наружу не отдаются.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FishSpeciesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void returnsSpeciesSortedByNameWithoutToken() throws Exception {
        jdbcTemplate.update("DELETE FROM fish_species");
        // порядок вставки специально не по алфавиту: сортировать должна база
        jdbcTemplate.update("""
                INSERT INTO fish_species (name_ru, name_lat, slug, description, min_catch_size_cm, is_protected, release_advice) VALUES
                ('Щука', 'Esox lucius', 'pike', 'Хищник', 32.0, false, NULL),
                ('Окунь', 'Perca fluviatilis', 'perch', NULL, NULL, false, NULL),
                ('Стерлядь', 'Acipenser ruthenus', 'sterlet', NULL, NULL, true, 'Отпустить сразу')
                """);

        // запрос без заголовка Authorization: справочник открыт по контракту
        mockMvc.perform(get("/api/v1/fish-species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nameRu").value("Окунь"))
                .andExpect(jsonPath("$[1].nameRu").value("Стерлядь"))
                .andExpect(jsonPath("$[2].nameRu").value("Щука"))
                .andExpect(jsonPath("$[1].isProtected").value(true))
                .andExpect(jsonPath("$[1].releaseAdvice").value("Отпустить сразу"))
                .andExpect(jsonPath("$[2].minCatchSizeCm").value(32.0))
                // описание и slug в справочнике не отдаём
                .andExpect(jsonPath("$[2].description").doesNotExist())
                .andExpect(jsonPath("$[2].slug").doesNotExist());
    }
}
