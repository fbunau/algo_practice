package com.algopractice

import cats.Show
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.show._
import cats.syntax.option._
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.vector._
import cats.instances.tuple._
import cats.instances.option._
import cats.syntax.eq._
import cats.instances.int._
import cats.effect._
import cats.syntax.functor._
import cats.instances.string.catsStdShowForString
import com.algopractice.DatasetParser.{ArrayOfInt, SingleNumber_ArrayOfInt}
import com.algopractice.util.DebugUtil
import com.algopractice.util.DebugUtil.{error, info, log, logResult}
import org.parboiled2.{ParseError, ParserInput}
import shapeless.{::, HNil}

import scala.annotation.tailrec
import scala.io.Source
import scala.language.higherKinds
import scala.util.{Failure, Success}

object Runner extends IOApp {

  import com.codility.lessons.CyclicRotation

  private val InputFile = "src\\main\\resources\\input.txt"
  private val ExpectedFile = "src\\main\\resources\\expected.txt"

  //////////////////////////////////////////////////////////////////
  // Solution switch

  type Input = (Int, Vector[Int])
  type Output = Vector[Int]

  private val inputDatasetParser: ParserInput => DatasetParser[List[Input] :: HNil] = SingleNumber_ArrayOfInt
  private val outputDatasetParser: ParserInput => DatasetParser[List[Output] :: HNil] = ArrayOfInt

  private val solution: Input => Output = ((k: Int, v: Vector[Int]) => CyclicRotation.solution(v.toArray, k)).tupled.andThen(_.toVector)

  //////////////////////////////////////////////////////////////////
  private val specificTestRun: Option[Int] = None//4.some
  //////////////////////////////////////////////////////////////////


  def run(args: List[String]): IO[ExitCode] = {

    def selectTestCaseData[A](l: List[A]) = specificTestRun.map(index => List(l(index-1))).getOrElse(l)

    val p = for {
      inputs <- readData[Input](InputFile)(inputDatasetParser)
      expected <- readData[Output](ExpectedFile)(outputDatasetParser)

      selectedInputs = selectTestCaseData(inputs)
      selectedExpected = selectTestCaseData(expected)

      _ <- info(s"Found: [${selectedInputs.size}] tests\n")

      testRuns <- (selectedInputs.zipWithIndex, selectedExpected).zipped.map {
        case ((input, caseNb), expected) =>
          for {
            _ <- info(s"Running #${caseNb + 1} ..")

            result <- IO.pure(solution(input))
            _ <- log(input, "INPUT")
            _ <- logResult(result)

            testPass = (result === expected)
            _ <- if (testPass) info("SUCCESS\n") else error("FAILED") *> info(show"Expected: $expected\n")
          } yield testPass
      }.sequence
    } yield testRuns

    p.attempt.flatMap {
      case Left(e) => error(DebugUtil.throwableStringStackTrace(e)).as(ExitCode.Error)
      case Right(results) =>
        val allTestsCheck = results.fold(true)(_ & _)
        if (allTestsCheck) info("ALL tests SUCCESS").as(ExitCode.Success)
        else error("Some tests FAILED").as(ExitCode.Error)
    }

  }

  private def readData[A](filePath: String)(buildParser: ParserInput => DatasetParser[List[A] :: HNil]): IO[List[A]] = {
    val source = Source.fromFile(filePath)
    val data = source.getLines.mkString("")
    val parser = buildParser(data)

    parser.datasetFormat.run() match {
      case Success(x) => IO(source.close) *> IO.pure(x)
      case Failure(e: ParseError) => IO.raiseError[List[A]](new Exception(s"Failed to parse data from: $filePath. Parser error: ${parser.formatError(e)}"))
      case Failure(e) => IO.raiseError[List[A]](new Exception(s"Failed to parse data from: $filePath. Parser error: $e"))
    }

  }

}



