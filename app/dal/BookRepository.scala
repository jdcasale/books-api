package dal

/**
  * Created by jdcasale on 6/3/17.
  */

import javax.inject.{Inject, Singleton}

import models.{Author, Book, BookAuthor}
import play.api.db.slick.DatabaseConfigProvider
import play.api.Logger
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}



/**
  * A repository for books.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this for you.
  */
@Singleton
class BookRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import driver.api._

  /**
    * Here we define the table. It will have a title of books
    */
  private class BooksTable(tag: Tag) extends Table[Book](tag, "Books") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("book_id", O.PrimaryKey, O.AutoInc)

    /** The title column */
    def title = column[String]("title")


    /** The description column */
    def description = column[String]("description")

    /**
      * This is the tables evolutions.default "projection".
      *
      * It defines how the columns are converted to and from the Person object.
      *
      * In this case, we are simply passing the id, title and page parameters to the Person case classes
      * apply and unapply methods.
      */
    def * = (/**id,**/ title, description) <> ((Book.apply _).tupled, Book.unapply)
  }

  private class BookAuthorsTable(tag: Tag) extends Table[BookAuthor](tag, "BookAuthors") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def author_id = column[Long]("author_id")
    def book_id = column[Long]("book_id")

    def * = (author_id, book_id) <> ((BookAuthor.apply _).tupled, BookAuthor.unapply)
  }

  /**
    * The starting point for all queries on the books table.
    */
  private val books = TableQuery[BooksTable]
  private val bookAuthors = TableQuery[BookAuthorsTable]

  def createWithAuthor(title: String, description: String, authorNames: List[String]): Future[Book] = {
    var aid = 0L; var bid = 0L

    /** First create a book object and get it's book_id */
    if(authorNames.isEmpty) throw new Exception
    val b = db.run(
      (books.map(p => (p.title, p.description))
      returning books.map(_.id)
      into ((titleDescription, bookId) => {
      bid = bookId
      Book(titleDescription._1, titleDescription._2)
    })
      ) += (title, description)
    )

    authorNames.foreach(s => Logger.error(s"name: $s"))
    /** Next, create a row in the Authors table and a row in the book_authors table and get its book_id */
    authorNames.foreach(

      authorName => {
        db.run(
          (authors.map(p => p.name)
          returning authors.map(_.id)
          into (
          (name, authorId) => {
            aid = authorId
            Author(name)
          })) += (authorName)
        )
        db.run(bookAuthors.map(p => (p.author_id, p.book_id)) += (aid, bid))
      }
    )
    b
  }

  /**
    * Create a book with the given title and description.
    *
    * This is an asynchronous operation, it will return a future of the created book, which can be used to obtain the
    * id for that book.
    */
  def create(title: String, description: String, authors: List[String]): Future[Book] = db.run {
    // We create a projection of just the title and description columns, since we're not inserting a value for the id column
    (books.map(p => (p.title, p.description))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning books.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((titleDescription, bookId) => Book(titleDescription._1, titleDescription._2))
      // And finally, insert the person into the database
      ) += (title, description)
  }

  /**
    * List all the books in the database.
    */
  def listBooks(): Future[Seq[Book]] = db.run {
    books.result
  }

  /**
    * Search for books with author, title, or description matching a keyword
    */
  def search(searchStr: String): Future[Seq[Book]] = {
    implicit val getUserResult = GetResult(r => Book(r.<<, r.<<))
    val q = sql"""
              SELECT DISTINCT Books.title, Books.description
              FROM  BookAuthors RIGHT JOIN Books
              ON BookAuthors.book_id = Books.book_id
              LEFT JOIN Authors ON BookAuthors.author_id = Authors.author_id
              WHERE Books.title LIKE '%#$searchStr%'
              OR Books.description LIKE '%#$searchStr%'
              OR Authors.name LIKE '%#$searchStr%'
              """.as[Book]

    db.run(q)
  }

  /**
    * Utility method for debugging, show all book-author combinations
    */
  def searchBA(searchStr: String): Future[Seq[BookAuthor]] = {
    db.run(bookAuthors.result)
  }

  /**
    * Here we define the table. It will have a name of authors
    */
  private class AuthorsTable(tag: Tag) extends Table[Author](tag, "Authors") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("author_id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")

    /**
      * This is the tables evolutions.default "projection".
      *
      * It defines how the columns are converted to and from the Person object.
      *
      * In this case, we are simply passing the id, name and page parameters to the Person case classes
      * apply and unapply methods.
      */
    def * = (/**id,**/ name) <> (Author.apply, Author.unapply)
  }

  /**
    * The starting point for all queries on the authors table.
    */
  private val authors = TableQuery[AuthorsTable]

  /**
    * Create a book with the given name and description.
    *
    * This is an asynchronous operation, it will return a future of the created book, which can be used to obtain the
    * id for that book.
    */
  def createAuthor(name: String): Future[Author] = db.run {
    // We create a projection of just the name and description columns, since we're not inserting a value for the id column
    (authors.map(p => p.name)
      // Now define it to return the id, because we want to know what id was generated for the person
      returning authors.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((name, authorId) => Author(name))
      // And finally, insert the person into the database
      ) += name
  }

  /**
    * List all the authors in the database.
    */
  def listAuthors(): Future[Seq[Author]] = db.run {
    authors.result
  }

  /**
    * Update the description of an existing book.
    */
  def updateBookDescription(bookTitle: String, newDescription: String) = {
    var q = for{ b <- books if b.title === bookTitle } yield b.description
    db.run(q.update(newDescription))
  }

  /**
    * Update the name of an existing author.
    */
  def updateAuthorDescription(oldName: String, newName: String) = {
    var q = for{ a <- authors if a.name === oldName } yield a.name
    db.run(q.update(newName))
  }




}