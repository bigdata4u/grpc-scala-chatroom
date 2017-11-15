package chatroom

import brave.Tracing
import brave.grpc.GrpcTracing
import chatroom.AuthService.AuthenticationServiceGrpc
import chatroom.grpc.AuthServiceImpl
import chatroom.repository.UserRepository
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ServerBuilder, ServerInterceptors}
import zipkin.reporter.AsyncReporter
import zipkin.reporter.urlconnection.URLConnectionSender

import scala.concurrent.ExecutionContext

object AuthServer extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val repository = new UserRepository
    val algorithm = Algorithm.HMAC256("secret")
    val authServiceImpl = new AuthServiceImpl(repository, "auth-issuer", algorithm)

    val reporter = AsyncReporter.create(URLConnectionSender.create(EnvVars.ZIPKIN_URL))
    val tracing = GrpcTracing.create(Tracing.newBuilder.localServiceName("auth-service").reporter(reporter).build)

    val server = ServerBuilder.forPort(EnvVars.AUTH_SERVICE_PORT)
      .addService(ServerInterceptors.intercept(
        AuthenticationServiceGrpc.bindService(authServiceImpl, ExecutionContext.global),
        tracing.newServerInterceptor())
      )
      .build

    server.start()
    logger.info("Server started on port " + EnvVars.AUTH_SERVICE_PORT)
    server.awaitTermination()

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        server.shutdownNow
      }
    })
  }

}
