package com.training.bookmanager.dto;

import java.time.LocalDateTime;

import com.training.bookmanager.model.Book;

public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String publishedDate;
    private String isbn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookResponse fromEntity(Book book) {
        BookResponse res = new BookResponse();
        res.id = book.getId();
        res.title = book.getTitle();
        res.author = book.getAuthor();
        res.publisher = book.getPublisher();
        res.publishedDate = book.getPublishedDate();
        res.isbn = book.getIsbn();
        res.createdAt = book.getCreatedAt();
        res.updatedAt = book.getUpdatedAt();
        return res;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public String getIsbn() {
        return isbn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
