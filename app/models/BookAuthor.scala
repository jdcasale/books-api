package models

import play.api.libs.json._

/**
  * Created by jdcasale on 6/2/17.
  */
case class BookAuthor(authorId: Long, bookId: Long)  {
}

object BookAuthor {

  implicit val bookAuthorFormat = Json.format[BookAuthor]
}


