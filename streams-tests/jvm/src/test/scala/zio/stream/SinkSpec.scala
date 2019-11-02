package zio.stream

import java.util.concurrent.TimeUnit

import zio._
import zio.clock.Clock
import zio.duration._
import zio.stream.SinkUtils._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestClock

import scala.{Stream => _}

object SinkSpec
    extends ZIOBaseSpec(
      suite("SinkSpec")(
        suite("Constructors")(
          testM("fromOutputStream") {
            import java.io.ByteArrayOutputStream

            val output = new ByteArrayOutputStream()
            val data   = "0123456789"
            val stream = Stream(Chunk.fromArray(data.take(5).getBytes), Chunk.fromArray(data.drop(5).getBytes))

            for {
              bytesWritten <- stream.run(ZSink.fromOutputStream(output))
            } yield assert(bytesWritten, equalTo(10)) && assert(
              new String(output.toByteArray, "UTF-8"),
              equalTo(data)
            )
          },
        ),
      )
    )
