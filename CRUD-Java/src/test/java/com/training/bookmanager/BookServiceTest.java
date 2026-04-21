package com.training.bookmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.training.bookmanager.dto.BookRequest;
import com.training.bookmanager.dto.BookUpdateRequest;
import com.training.bookmanager.model.Book;
import com.training.bookmanager.repository.BookRepository;
import com.training.bookmanager.service.BookService;

@SpringBootTest
class BookServiceTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    void testCreateBook() {
        BookRequest request = new BookRequest();
        request.setTitle("テスト書籍");
        request.setAuthor("テスト著者");
        request.setPublisher("テスト出版社");

        Book created = bookService.create(request);

        assertNotNull(created.getId());
        assertEquals("テスト書籍", created.getTitle());
        assertEquals("テスト著者", created.getAuthor());
        assertEquals("テスト出版社", created.getPublisher());
    }

    @Test
    void testFindAllEmpty() {
        List<Book> books = bookService.findAll();
        assertTrue(books.isEmpty());
    }

    @Test
    void testFindAllAfterCreate() {
        BookRequest req1 = new BookRequest();
        req1.setTitle("本A");
        req1.setAuthor("著者A");
        bookService.create(req1);

        BookRequest req2 = new BookRequest();
        req2.setTitle("本B");
        req2.setAuthor("著者B");
        bookService.create(req2);

        List<Book> books = bookService.findAll();
        assertEquals(2, books.size());
    }

    @Test
    void testFindById() {
        BookRequest request = new BookRequest();
        request.setTitle("検索対象");
        request.setAuthor("検索著者");
        Book created = bookService.create(request);

        Book found = bookService.findById(created.getId());
        assertNotNull(found);
        assertEquals("検索対象", found.getTitle());
    }

    @Test
    void testFindByIdNotFound() {
        Book result = bookService.findById(9999L);
        assertNull(result);
    }

    @Test
    void testUpdateBook() {
        BookRequest request = new BookRequest();
        request.setTitle("更新前");
        request.setAuthor("著者");
        Book created = bookService.create(request);

        BookUpdateRequest updateReq = new BookUpdateRequest();
        updateReq.setTitle("更新後");
        Book updated = bookService.update(created.getId(), updateReq);

        assertNotNull(updated);
        assertEquals("更新後", updated.getTitle());
        assertEquals("著者", updated.getAuthor());
    }

    @Test
    void testUpdateBookNotFound() {
        BookUpdateRequest updateReq = new BookUpdateRequest();
        updateReq.setTitle("存在しない");
        Book result = bookService.update(9999L, updateReq);
        assertNull(result);
    }

    @Test
    void testDeleteBook() {
        BookRequest request = new BookRequest();
        request.setTitle("削除対象");
        request.setAuthor("著者");
        Book created = bookService.create(request);

        boolean result = bookService.delete(created.getId());
        assertTrue(result);
        assertNull(bookService.findById(created.getId()));
    }

    @Test
    void testDeleteBookNotFound() {
        boolean result = bookService.delete(9999L);
        assertFalse(result);
    }
}
