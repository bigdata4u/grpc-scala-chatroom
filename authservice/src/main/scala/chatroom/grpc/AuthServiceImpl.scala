package chatroom.grpc



import chatroom.AuthService.{AuthenticationRequest, AuthenticationServiceGrpc, AuthorizationRequest}
import chatroom.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.grpc.Status

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class AuthServiceImpl(repository: UserRepository,
                      issuer: String,
                      algorithm: Algorithm) extends AuthenticationServiceGrpc.AuthenticationService {


  val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(issuer).build

  def generateToken(username: String): String = JWT.create.withIssuer(issuer).withSubject(username).sign(algorithm)

  def jwtFromToken(token: String): DecodedJWT = verifier.verify(token)

  // TODO Override authenticate methods

  // TODO Override authorization method
  override def authenticate(request: AuthenticationRequest): Future[chatroom.AuthService.AuthenticationResponse] = {

    val user = repository.findUser(request.username)
    if (user == null || !(user.password == request.password)) {
      Future.failed(Status.UNAUTHENTICATED.asRuntimeException)
    }

    val token = generateToken(request.username)
    Future.successful(chatroom.AuthService.AuthenticationResponse(token))
  }

  override def authorization(request: AuthorizationRequest): Future[chatroom.AuthService.AuthorizationResponse] = {
    val jwt = jwtFromToken(request.token)
    val username = jwt.getSubject
    val user = repository.findUser(username)
    if (user == null) {
     Future(Status.UNAUTHENTICATED.asRuntimeException())
    }

    val seq = user.roles.toSeq
    println(s"user:$user user.roles:${user.roles} seq roles:$seq")


    Future.successful(chatroom.AuthService.AuthorizationResponse(username, seq))
  }
}
