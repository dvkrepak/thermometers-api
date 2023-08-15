package api

object Validators {

  def requireCorrectDateFormat(date: String): Boolean = {
    date.matches("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{3}-\d{4}""")
  }

}
