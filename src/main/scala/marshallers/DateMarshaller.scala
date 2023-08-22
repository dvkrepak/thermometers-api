package marshallers

import play.api.libs.json.{Reads, Writes}

import java.util.Date

/**
 * A trait that provides JSON marshalling support for `Date` objects
 */
trait DateMarshaller {
  // Human-readable representation of ISO 8601 date format
  implicit val dateReads: Reads[Date] = Reads.dateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val dateWrites: Writes[Date] = Writes.dateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}
