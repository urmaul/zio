package zio.stream

import zio.test.{ Gen, GenZIO, Sized }
import zio.random.Random
import zio._
import ZStream.Pull
import scala.concurrent.ExecutionContext

trait StreamUtils extends ChunkUtils with GenZIO {
  def streamGen[R <: Random, A](a: Gen[R, A], max: Int): Gen[R with Sized, Stream[String, A]] =
    Gen.oneOf(failingStreamGen(a, max), pureStreamGen(a, max))

  def pureStreamGen[R <: Random, A](a: Gen[R, A], max: Int): Gen[R with Sized, Stream[Nothing, A]] =
    max match {
      case 0 => Gen.const(Stream.empty)
      case n =>
        Gen.oneOf(
          Gen.const(Stream.empty),
          Gen.int(1, n).flatMap(Gen.listOfN(_)(a)).map(Stream.fromIterable)
        )
    }

  def failingStreamGen[R <: Random, A](a: Gen[R, A], max: Int): Gen[R with Sized, Stream[String, A]] =
    max match {
      case 0 => Gen.const(ZStream.fromEffect(IO.fail("fail-case")))
      case _ =>
        Gen
          .int(1, max)
          .flatMap(
            n =>
              for {
                i  <- Gen.int(0, n - 1)
                it <- Gen.listOfN(n)(a)
              } yield ZStream.unfoldM((i, it)) {
                case (_, Nil) | (0, _) => IO.fail("fail-case")
                case (n, head :: rest) => IO.succeed(Some((head, (n - 1, rest))))
              }
          )
    }

  def pureStreamEffectGen[R <: Random, A](a: Gen[R, A], max: Int): Gen[R with Sized, StreamEffect[Any, Nothing, A]] =
    Gen.int(0, max).flatMap(Gen.listOfN(_)(a)).map(StreamEffect.fromIterable)

  def failingStreamEffectGen[R <: Random, A](a: Gen[R, A], max: Int): Gen[R with Sized, StreamEffect[Any, String, A]] =
    for {
      n  <- Gen.int(1, max)
      it <- Gen.listOfN(n)(a)
    } yield StreamEffect.unfold((n, it)) {
      case (_, Nil) | (0, _) => None
      case (n, head :: rest) => Some((head, (n - 1, rest)))
    }

  def inParallel(action: => Unit)(implicit ec: ExecutionContext): Unit =
    ec.execute(() => action)

  def dropUntil[A](as: List[A])(f: A => Boolean): List[A] =
    as.dropWhile(!f(_)).drop(1)

  def takeUntil[A](as: List[A])(f: A => Boolean): List[A] =
    as.takeWhile(!f(_)) ++ as.dropWhile(!f(_)).take(1)

  def threePulls[R, E, A](pull: Pull[R, E, A]): ZIO[R, Nothing, List[Either[Option[E], A]]] =
    for {
      e1 <- pull.either
      e2 <- pull.either
      e3 <- pull.either
    } yield List(e1, e2, e3)
}

object StreamUtils extends StreamUtils with GenUtils {
  val streamOfBytes   = Gen.small(streamGen(Gen.anyByte, _))
  val streamOfInts    = Gen.small(streamGen(intGen, _))
  val streamOfStrings = Gen.small(streamGen(stringGen, _))

  val listOfInts = Gen.listOf(intGen)

  val pureStreamOfBytes   = Gen.small(pureStreamGen(Gen.anyByte, _))
  val pureStreamOfInts    = Gen.small(pureStreamGen(intGen, _))
  val pureStreamOfStrings = Gen.small(pureStreamGen(stringGen, _))
}
