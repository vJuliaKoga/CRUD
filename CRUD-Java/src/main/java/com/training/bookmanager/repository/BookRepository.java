package com.training.bookmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.training.bookmanager.model.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
}
