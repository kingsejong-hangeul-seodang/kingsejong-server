package com.kingsejong.domain.example;

import com.kingsejong.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ExampleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 만들고_조회하면_같은_값이_나온다() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"바다\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.name").value("바다"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andReturn();

        String body = created.getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/v1/examples/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("바다"));
    }

    @Test
    void 이름이_비어_있으면_422와_details를_준다() throws Exception {
        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void JSON이_깨지면_400을_준다() throws Exception {
        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void 없는_id면_404를_준다() throws Exception {
        mockMvc.perform(get("/api/v1/examples/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
