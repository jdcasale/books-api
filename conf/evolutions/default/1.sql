---# Books schema

# --- !Ups

--- The descriptions in the given file are preeeeeety long (5000 characters wasnt long enough)
CREATE TABLE Books (
    book_id bigint(20) NOT NULL AUTO_INCREMENT,
    title varchar(255) NOT NULL,
    description varchar(10000) NOT NULL,
    PRIMARY KEY (book_id),
    UNIQUE KEY(title)
);

--- https://stackoverflow.com/questions/20958/list-of-standard-lengths-for-database-fields
--- suggests max 50 characters for each of given name and surname, so I used 101 characters for the name field.
CREATE TABLE Authors (
    author_id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(101) NOT NULL,
    PRIMARY KEY(author_id),
    UNIQUE KEY(name)
);

CREATE TABLE BookAuthors (
    book_id bigint(20) NOT NULL,
    author_id bigint(20) NOT NULL,
    FOREIGN KEY (book_id) REFERENCES Books(book_id),
    FOREIGN KEY (author_id) REFERENCES Authors(author_id)
);


# --- !Downs

DROP TABLE Authors;
DROP TABLE Books;
DROP TABLE BookAuthors;
