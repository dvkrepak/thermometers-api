package api

object Validators {

  private def getMaxDayAmount(monthInt: Int, yearInt: Int): Int = {
    if (monthInt == 2) {
      if (isLeapYear(yearInt)) 29
      else 28
    } else if (List(4, 6, 9, 11).contains(monthInt)) 30 else 31
  }

  private def isLeapYear(year: Int): Boolean = {
      (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0)
  }

  def requireCorrectDate(date: String): Boolean = {
    val dateFormatRegex = """(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}).(\d{3})-(\d{4})""".r


    date match {
      case dateFormatRegex(year, month, day, hour, minutes, seconds, milliseconds, _) =>
        val yearInt = year.toInt
        val monthInt = month.toInt
        val dayInt = day.toInt
        val hourInt = hour.toInt
        val minutesInt = minutes.toInt
        val secondsInt = seconds.toInt

        val StartYear = 1970

        yearInt >= StartYear &&
          1 <= monthInt && monthInt <= 12 &&
          1 <= dayInt && dayInt <= getMaxDayAmount(monthInt, yearInt) &&
          0 <= hourInt && hourInt <= 23 &&
          0 <= minutesInt && minutesInt <= 59 &&
          0 <= secondsInt && secondsInt <= 59 &&
          milliseconds.length == 3
      case _ => false
    }
  }

  private def isHexLetterOrDigit(c: Char): Boolean = {
    c.isDigit || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
  }

  def requireCorrectId(id: String): Boolean = {
    id.length == 24 && id.forall(isHexLetterOrDigit)
  }
}
