package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface BookRepository extends MongoRepository<Book, String>, BookRepositoryCustomMethods {

    Page<Book> findAllByAuthorOrderByCreatedDateTimeDesc(Pageable page, String author);

    Page<Book> findAllByGenreOrderByCreatedDateTimeDesc(Pageable page, String genre);

    Page<Book> findAllByOrderByCreatedDateTimeDesc(Pageable page);

    Page<Book> findByRatingOrderByCreatedDateTimeDesc(Pageable page, Book.Rating rating);

    @Query("{ 'createdBy.fullName' : ?0 }")
    Page<Book> findByReaderOrderByCreatedDateTimeDesc(Pageable page, String reader);

}
