package models

import play.api.libs.json._

/**
  * Created by jdcasale on 6/2/17.
  */
case class Book(name: String, /**authors: List[String],**/ description: String)  {
}

object Book {

  implicit val bookFormat = Json.format[Book]
}


