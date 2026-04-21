package com.training.bookmanager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.training.bookmanager.dto.BookRequest;
import com.training.bookmanager.dto.BookUpdateRequest;
import com.training.bookmanager.model.Book;
import com.training.bookmanager.repository.BookRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    public Book create(BookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublishedDate(request.getPublishedDate());
        book.setIsbn(request.getIsbn());
        return bookRepository.save(book);
    }

    public Book update(Long id, BookUpdateRequest request) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return null;
        }

        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getPublisher() != null) {
            book.setPublisher(request.getPublisher());
        }
        if (request.getPublishedDate() != null) {
            book.setPublishedDate(request.getPublishedDate());
        }
        if (request.getIsbn() != null) {
            book.setIsbn(request.getIsbn());
        }

        return bookRepository.save(book);
    }

    public boolean delete(Long id) {
        if (!bookRepository.existsById(id)) {
            return false;
        }
        bookRepository.deleteById(id);
        return true;
    }
}
