package com.examples.spark

import trembita._
import trembita.ql._
import trembita.spark._
import cats.effect.{ExitCode, IO, IOApp}
import org.apache.spark.sql._
import cats.implicits._
import com.examples.kernel.NumbersReport
import cats.effect.Console.io._
import scala.concurrent.duration._
import shapeless.syntax.singleton._

object QLExample extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO(
      SparkSession
        .builder()
        .master("spark://spark-master:7077")
        .appName("trembita-spark")
        .getOrCreate()
    ).bracket(use = { implicit spark: SparkSession =>
        import spark.implicits._
        implicit val timeout: AsyncTimeout = AsyncTimeout(5.minutes)
        val numbers: DataPipelineT[IO, Long, Spark] =
          Input
            .sequentialF[IO, Seq]
            .create(IO { 1L to 2000L })
            .to[Spark]

        val result = numbers
          .where(_ > 5)
          .groupBy(
            expr[Long](_ % 2 == 0) as "divisible by 2",
            expr[Long](_ % 3 == 0) as "divisible by 3",
            expr[Long](_ % 4) as "reminder of 4",
            expr[Long](_.toString.length) as "length"
          )
          .aggregate(
            expr[Long](num => (num * num).toDouble) agg avg as "square",
            col[Long] agg count as "count",
            expr[Long](num => num * num * num * num) agg sum as "^4",
            expr[Long](_.toString) agg sum as "some name",
            expr[Long](_.toDouble) agg stdev as "STDEV"
          )
          .having(agg[Long]("count")(_ > 7))
          .compile
          .as[NumbersReport]

        result.into(Output.array).run.flatMap(vs => putStrLn(vs.mkString("\n")))
      })(
        release = spark =>
          IO {
            spark.close()
        }
      )
      .map(_ => ExitCode.Success)
}
