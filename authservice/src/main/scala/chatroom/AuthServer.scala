package chatroom

import java.util.concurrent.Executors

import chatroom.AuthService.AuthenticationServiceGrpc
import chatroom.grpc.AuthServiceImpl
import chatroom.repository.UserRepository
import com.typesafe.scalalogging.LazyLogging

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
