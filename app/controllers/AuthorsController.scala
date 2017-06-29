package controllers

import play.api.mvc._
import play.api.i18n._
import play.api.data.Form
import play.api.data.Forms._
import play.api.Logger
import dal._

import scala.concurrent.{ExecutionContext, Future}
import javax.inject._

import play.api.libs.json.Json
/**
  * Created by jdcasale on 6/4/17.
  */
class AuthorsController  @Inject()(repo: BookRepository, val messagesApi: MessagesApi)
                                  (implicit ec: ExecutionContext) extends Controller with I18nSupport{
  /**
    * The mapping for the author form.
    */
  val authorForm: Form[CreateAuthorForm] = Form {
    mapping(
      "name" -> nonEmptyText
    )(CreateAuthorForm.apply)(CreateAuthorForm.unapply)
  }

  /**
    * The index action.
    */
  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  /**
    * The add author action.
    *
    * This is asynchronous, since we're invoking the asynchronous methods on BooksRepository
    */
  def addAuthor = Action.async { implicit request =>
    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle success.
    authorForm.bindFromRequest.fold(
      // The error function. We return the index page with the error form, which will render the errors.
      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
      // a future because the person creation function returns a future.
      errorForm => {
        Future.successful(Ok(views.html.index()))
      },
      // There were no errors in the from, so create the person.
      author => {
        repo.createAuthor(author.name).map { _ =>
          // If successful, we simply redirect to the index page.
          Redirect(routes.BooksController.index)
        }
      }
    )
  }


  /**
    * A REST endpoint that gets all the authors as JSON.
    */
  def getAuthors = Action.async {
    repo.listAuthors().map { Authors =>
      Ok(Json.toJson(Authors))
    }
  }

}
