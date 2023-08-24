package api

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ValidatorsTest extends AnyWordSpec with Matchers {

  "requireCorrectDate" should {
    "return true for a valid date format" in {
      // Arrange
      val validDate = "2023-08-19T15:30:45.123-0400" // Valid date

      // Act
      val result = Validators.requireCorrectDate(validDate)

      // Assert
      result should be(true)
    }
    "return false for a date format without milliseconds" in {
      // Arrange
      val withoutMilliseconds = "2023-08-19T15:30:45-0400" // Missing milliseconds

      // Act
      val result = Validators.requireCorrectDate(withoutMilliseconds)

      // Assert
      result should be(false)
    }

    "return false for an empty date" in {
      // Arrange
      val emptyDate = "" // Empty string

      // Act
      val result = Validators.requireCorrectDate(emptyDate)

      // Assert
      result should be(false)
    }

    "return false for an unreal year" in {
      // Arrange
      val emptyDate = "20230-08-19T15:30:45.123-0400" // Year 20230

      // Act
      val result = Validators.requireCorrectDate(emptyDate)

      // Assert
      result should be(false)
    }

    "return false for an unreal hour" in {
      // Arrange
      val emptyDate = "2023-08-19T27:30:45.123-0400" // Hour 27

      // Act
      val result = Validators.requireCorrectDate(emptyDate)

      // Assert
      result should be(false)
    }

    "return true for an correct day in a leap year" in {
      // Arrange
      val emptyDate = "2020-02-29T23:30:45.123-0400" // February 29th in a leap year

      // Act
      val result = Validators.requireCorrectDate(emptyDate)

      // Assert
      result should be(true)
    }

    "return false for an incorrect day in a not leap year" in {
      // Arrange
      val emptyDate = "2021-02-29T23:30:45.123-0400" // February 29th in a leap year

      // Act
      val result = Validators.requireCorrectDate(emptyDate)

      // Assert
      result should be(false)
    }
  }

  "requireCorrectId" should  {
    "return true for a valid 24-character hexadecimal string" in {
      val validId = "5a1f3e27bc8a90678543d2f9"
      val result = Validators.requireCorrectId(validId)
      result should be(true)
    }

    "return false for a non-hexadecimal string" in {
      val nonHexId = "5a1f3e27bc8a90678543d2g9" // 'g' is not a valid hexadecimal character
      val result = Validators.requireCorrectId(nonHexId)
      result should be(false)
    }

    "return false for a string of incorrect length (shorter)" in {
      val shortId = "5a1f3e27bc8a9"
      val result = Validators.requireCorrectId(shortId)
      result should be(false)
    }

    "return false for a string of incorrect length (longer)" in {
      val longId = "5a1f3e27bc8a90678543d2f912" // Longer than 24 characters
      val result = Validators.requireCorrectId(longId)
      result should be(false)
    }
  }
}