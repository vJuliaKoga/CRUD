package com.training.bookmanager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.training.bookmanager.dto.BookRequest;
import com.training.bookmanager.dto.BookResponse;
import com.training.bookmanager.dto.BookUpdateRequest;
import com.training.bookmanager.model.Book;
import com.training.bookmanager.service.BookService;

@RestController
@RequestMapping("/books")
public class BookApiController {

    private final BookService bookService;

    public BookApiController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookResponse> listBooks() {
        return bookService.findAll().stream()
                .map(BookResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
        Book book = bookService.findById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BookResponse.fromEntity(book));
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(
            @Validated @RequestBody BookRequest request) {
        Book created = bookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookResponse.fromEntity(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Validated @RequestBody BookUpdateRequest request) {
        Book updated = bookService.update(id, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BookResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        boolean deleted = bookService.delete(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
