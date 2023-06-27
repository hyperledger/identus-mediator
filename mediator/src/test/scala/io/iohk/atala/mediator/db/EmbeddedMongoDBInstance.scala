package io.iohk.atala.mediator.db
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.{ImmutableMongod, Mongod, RunningMongodProcess}
import de.flapdoodle.embed.process.io.ProcessOutput
import de.flapdoodle.reverse.TransitionWalker
import de.flapdoodle.reverse.transitions.Start
import zio.{Task, ZIO, ZLayer}

object EmbeddedMongoDBInstance {

  def layer(
      port: Int = 27077,
      hostIp: String = "localhost"
  ): ZLayer[Any, Throwable, TransitionWalker.ReachedState[RunningMongodProcess]] = {
    ZLayer.scoped(
      ZIO.acquireRelease(
        ZIO.attemptBlocking {
          Mongod
            .builder()
            .processOutput(Start.to(classOf[ProcessOutput]).initializedWith(ProcessOutput.silent()))
            .net(Start.to(classOf[Net]).initializedWith(Net.of(hostIp, port, false)))
            .build()
            .start(Version.Main.V6_0)
        }
      )(process => ZIO.succeed(process.close()))
    )
  }

}
