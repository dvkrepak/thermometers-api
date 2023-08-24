package simulators

import org.mongodb.scala.bson.ObjectId

import java.util.Date

case class ThermometerAction(_id: ObjectId = new ObjectId(),
                             thermometerId: ObjectId,
                             temperature: Option[Int] = None,
                             created_at: Date = new Date())
