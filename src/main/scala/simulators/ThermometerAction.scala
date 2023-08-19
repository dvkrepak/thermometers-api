package simulators

import org.mongodb.scala.bson.ObjectId

import java.util.Date

case class ThermometerAction(id: ObjectId,
                             thermometerId: ObjectId,
                             temperature: Option[Int] = None,
                             created_at: Date)

object ThermometerAction {
  def apply(thermometerId: ObjectId, temperature: Option[Int]): ThermometerAction = {
    val id = new ObjectId()
    val createdAt = new Date()
    ThermometerAction(id, thermometerId, temperature, createdAt)
  }
}
