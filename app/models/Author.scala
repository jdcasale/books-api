package models

import play.api.libs.json._

/**
  * Created by jdcasale on 6/2/17.
  */
case class Author(name: String)  {
}

object Author {
  implicit val authorFormat = Json.format[Author]
}


