package chatroom.grpc


import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.grpc._
import org.slf4j.LoggerFactory

class JwtServerInterceptor(val issuer: String, algorithm: Algorithm) extends ServerInterceptor {

  private val logger = LoggerFactory.getLogger(classOf[JwtServerInterceptor])


  private val verifier = JWT.require(algorithm)
    .withIssuer(issuer)
    .build()

  override def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT], metadata: Metadata,
                                          serverCallHandler: ServerCallHandler[ReqT, RespT]) = {
    //  Get token from Metadata
    val token = metadata.get(Constant.JWT_METADATA_KEY)
    logger.info(s"interceptCall token: $token")

    if (token == null) {
      serverCall.close(Status.UNAUTHENTICATED.withDescription("JWT Token is missing from Metadata"), metadata)
      JwtServerInterceptor.NOOP_LISTENER[ReqT]
    }
    else {
      try {
        val jwt = verifier.verify(token)
        val ctx = Context.current.withValue(Constant.USER_ID_CTX_KEY, jwt.getSubject).withValue(Constant.JWT_CTX_KEY, jwt)
        logger.info(s"jwt.getPayload ${jwt.getPayload}")
        Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler)
      }
      catch  {
        case e:Exception =>
          logger.info("Verification failed - Unauthenticated!")
          serverCall.close(Status.UNAUTHENTICATED.withDescription(e.getMessage).withCause(e), metadata)
          JwtServerInterceptor.NOOP_LISTENER[ReqT]
      }
    }
  }
}

object JwtServerInterceptor {
  def NOOP_LISTENER[ReqT]() = new ServerCall.Listener[ReqT]() {}
}
