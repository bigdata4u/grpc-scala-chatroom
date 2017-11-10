package chatroom

import brave.Tracing
import brave.grpc.GrpcTracing
import chatroom.AuthService.AuthenticationServiceGrpc
import chatroom.ChatService.{ChatRoomServiceGrpc, ChatStreamServiceGrpc}
import chatroom.grpc.{ChatRoomServiceImpl, ChatStreamServiceImpl, JwtClientInterceptor, JwtServerInterceptor}
import chatroom.repository.ChatRoomRepository
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.scalalogging.LazyLogging
import io.grpc._
import zipkin.reporter.AsyncReporter
import zipkin.reporter.urlconnection.URLConnectionSender

import scala.concurrent.ExecutionContext

object ChatServer extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val repository = new ChatRoomRepository
    val jwtServerInterceptor = new JwtServerInterceptor("auth-issuer", Algorithm.HMAC256("secret"))

    val reporter = AsyncReporter.create(URLConnectionSender.create(EnvVars.ZIPKIN_URL))
    val tracing = GrpcTracing.create(Tracing.newBuilder.localServiceName("chat-service").reporter(reporter).build)

    val authChannel = ManagedChannelBuilder.forTarget(EnvVars.AUTH_SERVICE_URL)
      .intercept(new JwtClientInterceptor)
      .intercept(tracing.newClientInterceptor())
      .asInstanceOf[ManagedChannelBuilder[_]]
      .usePlaintext(true)
      .asInstanceOf[ManagedChannelBuilder[_]]
      .build()

    val authService = AuthenticationServiceGrpc.stub(authChannel)
    val chatRoomService = new ChatRoomServiceImpl(repository, authService)
    val chatStreamService = new ChatStreamServiceImpl(repository)

    val chatRoomServiceDefinition = ChatRoomServiceGrpc.bindService(new ChatRoomServiceImpl(repository,authService), ExecutionContext.global)
    val chatStreamServiceDefinition = ChatStreamServiceGrpc.bindService(new ChatStreamServiceImpl(repository), ExecutionContext.global)

    val server = ServerBuilder.forPort(EnvVars.CHAT_SERVICE_PORT)
                    .addService(ServerInterceptors.intercept(chatRoomServiceDefinition, jwtServerInterceptor))
                    .addService(ServerInterceptors.intercept(chatStreamServiceDefinition, jwtServerInterceptor))
                    .asInstanceOf[ServerBuilder[_]]
                    .build
                    .start

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        server.shutdownNow
        authChannel.shutdownNow
      }
    })
    logger.info("Server Started on port " + EnvVars.CHAT_SERVICE_PORT)
    server.awaitTermination()
  }
}
