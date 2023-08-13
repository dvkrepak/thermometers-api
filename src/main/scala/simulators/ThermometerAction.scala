package simulators

import org.mongodb.scala.bson.ObjectId

import java.util.Date

case class ThermometerAction(_id: ObjectId,
                             thermometer: Thermometer,
                             temperature: Option[Int] = None,
                             created_at: Date)

object ThermometerAction {
  def apply(thermometer: Thermometer, temperature: Option[Int]): ThermometerAction = {
    val id = new ObjectId()
    val createdAt = new Date()
    ThermometerAction(id, thermometer, temperature, createdAt)
  }
}
