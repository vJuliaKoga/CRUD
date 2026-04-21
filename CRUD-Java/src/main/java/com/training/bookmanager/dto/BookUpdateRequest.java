package com.training.bookmanager.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BookUpdateRequest {

    @Size(min = 1, max = 200, message = "タイトルは1~200文字で入力してください")
    private String title;

    @Size(min = 1, max = 100, message = "著者は1~100文字で入力してください")
    private String author;

    @Size(max = 100)
    private String publisher;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "日付はYYYY-MM-DD形式で入力してください")
    private String publishedDate;

    @Pattern(regexp = "^$|^\\d{10}(\\d{3})?$", message = "ISBNは10桁または13桁の数字で入力してください")
    private String isbn;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
}
