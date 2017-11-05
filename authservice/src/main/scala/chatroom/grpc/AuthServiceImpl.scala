package chatroom.grpc



import chatroom.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.grpc.Status

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

// TODO Extend gRPC's AuthenticationServiceGrpc.AuthenticationService
class AuthServiceImpl(repository: UserRepository,
                      issuer: String,
                      algorithm: Algorithm)  {


  val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(issuer).build

  def generateToken(username: String): String = JWT.create.withIssuer(issuer).withSubject(username).sign(algorithm)

  def jwtFromToken(token: String): DecodedJWT = verifier.verify(token)

  // TODO Override authenticate methods
  // override def authenticate(request: AuthenticationRequest): Future[chatroom.AuthService.AuthenticationResponse]

  // TODO Override authorization method
  // override def authorization(request: AuthorizationRequest): Future[chatroom.AuthService.AuthorizationResponse]

}
