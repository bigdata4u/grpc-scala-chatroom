package chatroom.grpc

import com.auth0.jwt.interfaces.DecodedJWT
import com.typesafe.scalalogging.LazyLogging
import io.grpc._

class JwtClientInterceptor extends ClientInterceptor with LazyLogging {

  override def interceptCall[ReqT, RespT](methodDescriptor: MethodDescriptor[ReqT, RespT], callOptions: CallOptions, channel: Channel) = {
    new ForwardingClientCall.SimpleForwardingClientCall[ReqT, RespT](channel.newCall(methodDescriptor, callOptions)) {
      override def start(responseListener:ClientCall.Listener[RespT], headers:Metadata):Unit = {
        val jwt: DecodedJWT = Constant.JWT_CTX_KEY.get
        if (jwt != null) {
          headers.put(Constant.JWT_METADATA_KEY, jwt.getToken)
        }
        super.start(responseListener, headers)
      }
    }
  }
}
