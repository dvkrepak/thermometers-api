package api

object Validators {

  def requireCorrectDateFormat(date: String): Boolean = {
    date.matches("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{3}-\d{4}""")
  }

  private def isHexLetterOrDigit(c: Char): Boolean = {
    c.isDigit || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
  }

  def requireCorrectId(id: String): Boolean = {
    id.length == 24 && id.forall(isHexLetterOrDigit)
  }
}
