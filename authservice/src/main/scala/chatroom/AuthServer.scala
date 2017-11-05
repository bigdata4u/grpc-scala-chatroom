package chatroom

import java.util.concurrent.Executors

import chatroom.AuthService.AuthenticationServiceGrpc
import chatroom.grpc.AuthServiceImpl
import chatroom.repository.UserRepository
import com.auth0.jwt.algorithms.Algorithm
import io.grpc.{Context, ServerBuilder}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object AuthServer {
  private val logger = LoggerFactory.getLogger("AuthServer")

  def main(args: Array[String]): Unit = {
    val repository = new UserRepository
    // TODO Use ServerBuilder to create a new Server instance. Start it, and await termination.

    val algorithm = Algorithm.HMAC256("secret")
    val authServiceImpl = new AuthServiceImpl(repository, "auth-issuer", algorithm)
    val server = ServerBuilder.forPort(9091)
      .addService(AuthenticationServiceGrpc.bindService(authServiceImpl, ExecutionContext.global))
      .build.start

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        server.shutdownNow
      }
    })

    Context.current.addListener((context: Context) => {
      System.out.println("Call was cancelled!")
    }, Executors.newCachedThreadPool)

    logger.info("Server started on port 9091")
    server.awaitTermination()
  }

}
