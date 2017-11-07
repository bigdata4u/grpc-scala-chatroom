package chatroom

import chatroom.AuthService.AuthenticationServiceGrpc
import chatroom.grpc.AuthServiceImpl
import chatroom.repository.UserRepository
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.scalalogging.LazyLogging
import io.grpc.ServerBuilder

import scala.concurrent.ExecutionContext

object AuthServer extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val repository = new UserRepository
    val algorithm = Algorithm.HMAC256("secret")
    val authServiceImpl = new AuthServiceImpl(repository, "auth-issuer", algorithm)
    val server = ServerBuilder.forPort(9091)
      .addService(AuthenticationServiceGrpc.bindService(authServiceImpl, ExecutionContext.global))
      .build

    server.start()
    logger.info("Server started on port 9091")
    server.awaitTermination()

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        server.shutdownNow
      }
    })
  }

}
