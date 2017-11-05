package chatroom

import chatroom.AuthService.AuthenticationServiceGrpc
import chatroom.ChatService.{ChatRoomServiceGrpc, ChatStreamServiceGrpc}
import chatroom.grpc.{ChatRoomServiceImpl, ChatStreamServiceImpl, JwtClientInterceptor, JwtServerInterceptor}
import chatroom.repository.ChatRoomRepository
import com.auth0.jwt.algorithms.Algorithm
import io.grpc._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object ChatServer {

  private val logger = LoggerFactory.getLogger("ChatServer")

  def main(args: Array[String]): Unit = {
    val repository = new ChatRoomRepository
    val jwtServerInterceptor = new JwtServerInterceptor("auth-issuer", Algorithm.HMAC256("secret"))

    // TODO Initial tracer
    // TODO Add trace interceptor
    val authChannel = ManagedChannelBuilder.forTarget("localhost:9091")
      .intercept(new JwtClientInterceptor)
      .usePlaintext(true)
      .asInstanceOf[ManagedChannelBuilder[_]]
      .build()

    val authService = AuthenticationServiceGrpc.stub(authChannel)
    val chatRoomService = new ChatRoomServiceImpl(repository, authService)
    val chatStreamService = new ChatStreamServiceImpl(repository)

    // TODO Add JWT Server Interceptor, then later, trace interceptor

    val serviceDefinition = ChatRoomServiceGrpc.bindService(new ChatRoomServiceImpl(repository,authService), ExecutionContext.global)
    val streamServiceDefinition = ChatStreamServiceGrpc.bindService(new ChatStreamServiceImpl(repository), ExecutionContext.global)

    val server = ServerBuilder.forPort(9092)
                    .addService(ServerInterceptors.intercept(serviceDefinition, jwtServerInterceptor))
                    .addService(ServerInterceptors.intercept(streamServiceDefinition, jwtServerInterceptor))
                    .asInstanceOf[ServerBuilder[_]]
                    .build.start

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        server.shutdownNow
        authChannel.shutdownNow
      }
    })
    logger.info("Server Started on port 9092")
    server.awaitTermination()
  }
}
