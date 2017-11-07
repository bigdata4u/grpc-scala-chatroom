package chatroom.grpc

import chatroom.AuthService.AuthenticationServiceGrpc.AuthenticationService
import chatroom.AuthService.{AuthenticationRequest, AuthenticationResponse, AuthorizationRequest, AuthorizationResponse}
import chatroom.repository.UserRepository
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.{JWT, JWTVerifier}
import io.grpc.Status
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthServiceImpl(repository: UserRepository,
                      issuer: String,
                      algorithm: Algorithm) extends AuthenticationService {

  private val logger = LoggerFactory.getLogger(classOf[AuthServiceImpl])

  val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(issuer).build

  def generateToken(username: String): String = JWT.create.withIssuer(issuer).withSubject(username).sign(algorithm)

  def jwtFromToken(token: String): DecodedJWT = verifier.verify(token)

  override def authenticate(
      request: AuthenticationRequest): Future[AuthenticationResponse] = {
    val user = Future(Option(repository.findUser(request.username))) // looking for the user asynchronous
    user.flatMap {
      case Some(us) if us.password == request.password =>
        val token = generateToken(request.username)
        Future.successful(AuthenticationResponse(token))
      case _ =>
        Future.failed(Status.UNAUTHENTICATED.asRuntimeException)
    }
  }

  override def authorization(request: AuthorizationRequest): Future[AuthorizationResponse] = {
    (for {
      jwt <- Future(jwtFromToken(request.token))
      username = jwt.getSubject
      userOp <- Future(Option(repository.findUser(username))) if userOp.isDefined
      user = userOp.get
    } yield {
      val userRoles = user.roles.toSeq
      logger.info(s"user:$user user.roles:${user.roles} seq roles:$userRoles")
      AuthorizationResponse(username, userRoles)
    }) recoverWith {
      case _ => Future.failed(Status.UNAUTHENTICATED.asRuntimeException)
    }
  }
}
