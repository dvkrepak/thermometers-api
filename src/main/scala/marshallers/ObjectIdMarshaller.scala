package marshallers

import org.mongodb.scala.bson.ObjectId
import play.api.libs.json.{Format, JsResult, JsValue, Json}


/**
 * A trait that provides JSON marshalling support for `ObjectId`
 */
trait ObjectIdMarshaller {

  implicit val objectIdFormat: Format[ObjectId] = new Format[ObjectId] {
    override def reads(json: JsValue): JsResult[ObjectId] = {
      (json \ "$oid").validate[String].map(new ObjectId(_))
    }

    override def writes(o: ObjectId): JsValue =
      Json.obj("$oid" -> o.toString)
  }
}
