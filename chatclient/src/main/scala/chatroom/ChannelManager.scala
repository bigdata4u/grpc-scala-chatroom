package chatroom

import java.io.IOException

import chatroom.AuthService.{AuthenticationRequest, AuthenticationServiceGrpc, AuthorizationRequest}
import chatroom.AuthService.AuthenticationServiceGrpc.AuthenticationServiceBlockingStub
import chatroom.ChatService._
import chatroom.grpc.JwtCallCredential
import io.grpc.{ManagedChannel, ManagedChannelBuilder, Status, StatusRuntimeException}
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory

class ChannelManager {

  private val logger = LoggerFactory.getLogger(classOf[ChannelManager].getName)

  // Channels
  private var optAuthService: Option[AuthenticationServiceBlockingStub] = Option.empty[AuthenticationServiceBlockingStub]
  private var optAuthChannel: Option[ManagedChannel] = Option.empty[ManagedChannel]

  private var optJwtCallCredentials: Option[JwtCallCredential] = Option.empty[JwtCallCredential]
  private var optChatChannel: Option[ManagedChannel] = Option.empty[ManagedChannel]
  private var optChatRoomService: Option[ChatRoomServiceGrpc.ChatRoomServiceBlockingStub] = Option.empty[ChatRoomServiceGrpc.ChatRoomServiceBlockingStub]
  private var optToServer: Option[StreamObserver[ChatMessage]] = Option.empty[StreamObserver[ChatMessage]]

  /**
    * Initialize a managed channel to connect to the auth service.
    * Set the authChannel and authService
    */
  def initAuthService(): Unit = {
    logger.info("initializing auth service")
    // Build a new ManagedChannel
    val authChannel = ManagedChannelBuilder.forTarget("localhost:9091").usePlaintext(true).build
    optAuthChannel = Some(authChannel)

    // Get a new Blocking Stub
    val authService = AuthenticationServiceGrpc.blockingStub(authChannel)
    optAuthService = Some(authService)
  }

  def initChatChannel(token: String, clientOutput: String => Unit): Unit = {
    logger.info("initializing chat services with token: " + token)
    optJwtCallCredentials = Some(new JwtCallCredential(token))
    val chatChannel = ManagedChannelBuilder.forTarget("localhost:9092").usePlaintext(true).build
    optChatChannel = Some(chatChannel)

    initChatServices()
    initChatStream(clientOutput)
  }

  /**
    * Initialize Chat Services
    *
    */
  def initChatServices(): Unit = {
    for {
      chatChannel <- optChatChannel
      callCredentials <- optJwtCallCredentials
    } yield {
      val chatRoomService = ChatRoomServiceGrpc.blockingStub(chatChannel).withCallCredentials(callCredentials)
      optChatRoomService = Some(chatRoomService)
    }
  }

  /**
    * Initalize Chat Stream
    */
  def initChatStream(clientOutput: String => Unit): Unit = {

    val streamObserver = new StreamObserver[ChatMessageFromServer] {
      override def onError(t: Throwable): Unit = {
        logger.error("gRPC error", t)
        shutdown()
      }

      override def onCompleted(): Unit = {
        logger.error("server closed connection, shutting down...")
        shutdown()
      }

      override def onNext(chatMessageFromServer: ChatMessageFromServer): Unit = {
        try {
          clientOutput(s"${chatMessageFromServer.getTimestamp.seconds} ${chatMessageFromServer.from}> ${chatMessageFromServer.message}")
        }
        catch {
          case exc: IOException =>
            logger.error("Error printing to console", exc)
          case exc: Throwable => logger.error("grpc exception", exc)
        }
      }
    }

    for {
      chatChannel <- optChatChannel
      callCredentials <- optJwtCallCredentials
    } yield {
      val chatStreamService = ChatStreamServiceGrpc.stub(chatChannel).withCallCredentials(callCredentials)
      val toServer = chatStreamService.chat(streamObserver)
      optToServer = Some(toServer)
    }
  }

  def shutdown(): Unit = {
    logger.info("Closing Chat Channels")
    optChatChannel.map(chatChannel => chatChannel.shutdown())
    optAuthChannel.map(authChannel => authChannel.shutdown())
  }

  /**
    * Authenticate the username/password with AuthenticationService
    *
    * @param username
    * @param password
    * @return If authenticated, return the authentication token, else, return null
    */
  def authenticate(username: String, password: String): Option[String] = {
    logger.info("authenticating user: " + username)
    //  Call authService.authenticate(...) and retreive the token

    try {
      optAuthService.map {
        authService =>

          val authenticationReponse = authService.authenticate(new AuthenticationRequest(username, password))

          /*
            It is also possible to set a deadline to the call
            val authenticationReponse = authService
                      .withDeadlineAfter(1, TimeUnit.SECONDS)
                      .authenticate(new AuthenticationRequest(username, password))
          */

          val token = authenticationReponse.token

          // Retrieve all the roles with authService.authorization(...) and print out all the roles
          val authorizationResponse = authService.authorization(new AuthorizationRequest(token))
          logger.info("user has these roles: " + authorizationResponse.roles)

          // Return the token
          token
      }
    }
    // Catch StatusRuntimeException, because there could be Unauthenticated errors.
    catch {
      case e: StatusRuntimeException =>
        if (e.getStatus.getCode == Status.Code.UNAUTHENTICATED) {
          logger.error("user not authenticated: " + username, e)
        } else {
          logger.error("caught a gRPC exception", e)
        }
        // If there are errors, return None
        Option.empty[String]
    }
  }

  /**
    * List all the chat rooms from the server
    */
  def listRooms(clientOutput: String => Unit): Unit = {
    logger.info("listing rooms")
    optChatRoomService.foreach {
      chatRoomService =>
        val rooms = chatRoomService.getRooms(Empty.defaultInstance)
        rooms.foreach {
          room =>
            try
              clientOutput("Room: " + room.name)
            catch {
              case e: IOException =>
                e.printStackTrace()
            }
        }
    }
  }

  /**
    * Leave the room
    */
  def leaveRoom(room: String): Unit = {
    logger.info("leaving room: " + room)
    optToServer.foreach {
      chatStreamObserver =>
        val message = ChatMessage(`type` = MessageType.LEAVE, roomName = room)
        chatStreamObserver.onNext(message)
        logger.info("left room: " + room);
    }
  }

  /**
    * Join a Room
    *
    * @param room
    */
  def joinRoom(room: String): Unit = {
    logger.info("joinining room: " + room)
    optToServer.foreach {
      chatStreamObserver =>
        val message = ChatMessage(`type` = MessageType.JOIN, roomName = room)
        chatStreamObserver.onNext(message)
        logger.info("joined room: " + room)
    }
  }

  /**
    * Create Room
    *
    * @param room
    */
  def createRoom(room: String): Unit = {
    logger.info(s"create room: $room  optChatRoomService:$optChatRoomService")

    optChatRoomService.foreach {
      chatRoomService =>
        try {
          logger.info(s"chatRoomService: $chatRoomService")
          chatRoomService.createRoom(Room(name = room))
          logger.info("created room: " + room)
        }
        catch {
          case exc: Throwable => logger.error("Error creating room", exc)
        }
    }
  }

  /**
    * Send a message
    *
    * @param room
    * @param message
    */
  def sendMessage(room: String, message: String): Unit = {
    logger.info("sending chat message")
    // call toServer.onNext(...)
    optToServer match {
      case Some(toServer) =>
        val chatMessage = ChatMessage(MessageType.TEXT, room, message)
        toServer.onNext(chatMessage)
      case None =>
        logger.info("Not Connected")
    }
  }
}
