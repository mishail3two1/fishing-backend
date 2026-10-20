package com.fishing.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Проверяет, что ошибки API приходят в формате ProblemDetail и не раскрывают внутренние детали.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ErrorHandlingTest.FailingController.class})
class ErrorHandlingTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void unknownUrlReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unexpectedErrorReturnsProblemDetailWithoutDetails() throws Exception {
        mockMvc.perform(get("/test/fail"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Внутренняя ошибка сервера"));
    }

    // Контроллер только для теста: всегда падает, чтобы проверить обработчик непредвиденных ошибок.
    @RestController
    static class FailingController {
        @GetMapping("/test/fail")
        String fail() {
            throw new IllegalStateException("секретная внутренняя деталь");
        }
    }
}