package com.training.bookmanager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.training.bookmanager.repository.BookRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BookApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void testCreateBook() throws Exception {
        String json = """
                {"title": "API経由の書籍", "author": "API著者"}
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("API経由の書籍"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void testListBooks() throws Exception {
        String json1 = """
                {"title": "本1", "author": "著者1"}
                """;
        String json2 = """
                {"title": "本2", "author": "著者2"}
                """;

        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON).content(json1));
        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON).content(json2));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetBookNotFound() throws Exception {
        mockMvc.perform(get("/books/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBook() throws Exception {
        String createJson = """
                {"title": "更新前タイトル", "author": "著者"}
                """;

        MvcResult result = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String idStr = responseBody.split("\"id\":")[1].split(",")[0].trim();

        String updateJson = """
                {"title": "更新後タイトル"}
                """;

        mockMvc.perform(put("/books/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("更新後タイトル"))
                .andExpect(jsonPath("$.author").value("著者"));
    }

    @Test
    void testDeleteBook() throws Exception {
        String createJson = """
                {"title": "削除対象", "author": "著者"}
                """;

        MvcResult result = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String idStr = responseBody.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(delete("/books/" + idStr))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/books/" + idStr))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateBookValidationError() throws Exception {
        String json = """
                {"title": "", "author": "著者"}
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testCreateBookMissingField() throws Exception {
        String json = """
                {"title": "タイトルのみ"}
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }
}
