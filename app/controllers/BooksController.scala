package controllers

import play.api.mvc._
import play.api.i18n._
import play.api.data.Form
import play.api.data.Forms._
import dal.BookRepository

import scala.concurrent.{ExecutionContext, Future}
import javax.inject._

import play.api.libs.json.Json

/**
  * Created by jdcasale on 6/2/17.
  *
  * This controller creates an `Action` to handle HTTP requests to add resources (Books, Authors).
  *
  */
class BooksController @Inject()(repo: BookRepository, val messagesApi: MessagesApi)
                               (implicit ec: ExecutionContext) extends Controller with I18nSupport {

  /**
    * The mapping for the book form.
    */
  val bookForm: Form[CreateBookForm] = Form(
    mapping(
      "title" -> nonEmptyText,
      "description" -> nonEmptyText,
      "authors" -> list(nonEmptyText)
    )
    (CreateBookForm.apply)(CreateBookForm.unapply)
  )

  /**
    * The index action.
    */
  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  /**
    * The add person action.
    *
    * This is asynchronous, since we're invoking the asynchronous methods on PersonRepository.
    */
  def addBook = Action.async { implicit request =>
    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
    bookForm.bindFromRequest.fold(
      // The error function. We return the index page with the error form, which will render the errors.
      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
      // a future because the person creation function returns a future.
      errorForm => {
        Future.successful(Ok(views.html.books(errorForm)))
      },
      // There were no errors in the from, so create the person.
      book => {
        repo.createWithAuthor(book.title, book.description, book.authors).map { _ =>
          // If successful, we simply redirect to the index page.
          Redirect(routes.BooksController.index())
        }
      }
    )
  }

  def updateBook = Action.async { implicit request =>
    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
    bookForm.bindFromRequest.fold(
      // The error function. We return the index page with the error form, which will render the errors.
      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
      // a future because the person creation function returns a future.
      errorForm => {
        Future.successful(Ok(views.html.books(errorForm)))
      },
      // There were no errors in the from, so create the person.
      book => {
        repo.updateBookDescription(book.title, book.description).map { _ =>
          // If successful, we simply redirect to the index page.
          Redirect(routes.BooksController.index())
        }
      }
    )
  }

  /**
    * A REST endpoint that gets all the books as JSON.
    */
  def getBooks = Action.async {
    repo.listBooks().map { Books =>
      Ok(Json.toJson(Books))
    }
  }

  def searchBooks(searchStr: String) = Action.async {
    repo.search(searchStr).map { Books =>
      Ok(Json.toJson(Books))
    }
  }

  def searchBA(searchStr: String) = Action.async {
    repo.searchBA(searchStr).map { Books =>
      Ok(Json.toJson(Books))
    }
  }

  /**
    * Shortcut method to bulkload books from local file
    *
    */
  def loadFromLocalCsv(fname: String = "Sample_Books.csv") = Action.async {
    val bufferedSource = scala.io.Source.fromFile(s"public/$fname")
    val parsedApiLines = bufferedSource.getLines()
      .map(Parser.fromLine)
      .map(list => BookRow(list(0), list(2).filterNot(Set[Char]('\\', '\"')), list(1).split(',').toList))
      .foreach(book => repo.createWithAuthor(book.title, book.desc, book.authors))


    repo.listBooks().map { Books =>
      Ok(Json.toJson(Books))
    }
  }

  case class BookRow(title: String, desc: String, authors: List[String])


  object Parser {
    def fromLine(line: String): List[String] = {
      def recursive(
                     lineRemaining: String
                     , isWithinDoubleQuotes: Boolean
                     , valueAccumulator: String
                     , accumulator: List[String]
                   ): List[String] = {
        if (lineRemaining.isEmpty)
          valueAccumulator :: accumulator
        else if (lineRemaining.head == '"')
          if (isWithinDoubleQuotes)
            if (lineRemaining.tail.nonEmpty && lineRemaining.tail.head == '"')
            //escaped double quote
              recursive(lineRemaining.drop(2), true, valueAccumulator + '"', accumulator)
            else
            //end of double quote pair (ignore whatever's between here and the next comma)
              recursive(lineRemaining.dropWhile(_ != ','), false, valueAccumulator, accumulator)
          else
          //start of a double quote pair (ignore whatever's in valueAccumulator)
            recursive(lineRemaining.drop(1), true, "", accumulator)
        else if (isWithinDoubleQuotes)
        //scan to next double quote
          recursive(
            lineRemaining.dropWhile(_ != '"')
            , true
            , valueAccumulator + lineRemaining.takeWhile(_ != '"')
            , accumulator
          )
        else if (lineRemaining.head == ',')
        //advance to next field value
          recursive(
            lineRemaining.drop(1)
            , false
            , ""
            , valueAccumulator :: accumulator
          )
        else
        //scan to next double quote or comma
          recursive(
            lineRemaining.dropWhile(char => (char != '"') && (char != ','))
            , false
            , valueAccumulator + lineRemaining.takeWhile(char => (char != '"') && (char != ','))
            , accumulator
          )
      }

      if (line.nonEmpty)
        recursive(line, false, "", Nil).reverse
      else
        Nil
    }

    def fromLines(lines: List[String]): List[List[String]] =
      lines.map(fromLine)
  }

}
