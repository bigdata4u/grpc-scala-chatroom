package chatroom.grpc

import com.typesafe.scalalogging.LazyLogging
import io.grpc._

class JwtClientInterceptor extends ClientInterceptor with LazyLogging {

  override def interceptCall[ReqT, RespT](methodDescriptor: MethodDescriptor[ReqT, RespT], callOptions: CallOptions, channel: Channel) = {
    new ForwardingClientCall.SimpleForwardingClientCall[ReqT, RespT](channel.newCall(methodDescriptor, callOptions)) {
      override def start(responseListener:ClientCall.Listener[RespT], headers:Metadata):Unit = {
        // TODO Convert JWT Context to Metadata header
        super.start(responseListener, headers)
      }
    }
  }
}
