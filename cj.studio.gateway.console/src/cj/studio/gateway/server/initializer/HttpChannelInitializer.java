package cj.studio.gateway.server.initializer;

import javax.net.ssl.SSLEngine;

import cj.studio.ecm.IServiceProvider;
import cj.studio.gateway.server.handler.HttpChannelHandler;
import cj.studio.gateway.server.secure.ISSLEngineFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.List;

public class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
    IServiceProvider parent;

    public HttpChannelInitializer(IServiceProvider parent) {
        this.parent = parent;

    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        ISSLEngineFactory factory = (ISSLEngineFactory) parent.getService("$.server.sslEngine");
        if (factory.isEnabled()) {
            SSLEngine engine = factory.createSSLEngine();
            engine.setUseClientMode(false);
            pipeline.addLast("ssl", new SslHandler(engine));
        }
        pipeline.addLast(new HttpServerCodec());
        //HttpContentCompressor说明如下：
        //在HttpResponseEncoder之前加上 HttpContentCompressor 。response对象先进过HttpContentCompressor 压缩后，再经过HttpResponseEncoder进行序列化。
//		1：压缩主要是针对body进行压缩。http1.1不支持对header的压缩。
//		2：压缩后body的输出是trunked，而不是Content-length的形式。
//		如果不是Content-length的话浏览器在下载时不会显示进度条
        //对FullHttpResponse按照http协议进行序列化。判断header里面是ContentLength还是Trunked，然后body按照相应的协议进行序列化
        //冲突：对于文档输入由于选写回响应时并不清楚大小所以采用chunked，而对于资源文件已知大小且需要浏览器显示进度所以必须用Content-length的形式。所以要控制header中的Content-length
        //https://blog.csdn.net/xiangzhihong8/article/details/52029446
        pipeline.addLast(new HttpContentCompressor());
        pipeline.addLast(new ChunkedWriteHandler());
        HttpChannelHandler handler = new HttpChannelHandler(parent);
        pipeline.addLast(handler);
    }
}
//以下是采用不压缩，即跳过HttpContentCompressor而使得浏览器显示进度条
class DefaultHttpContentCompressor extends HttpContentCompressor {
    boolean isResource;//是资源文件

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
        //判断是否是资源文件，如果是则不压缩
        if (msg instanceof DefaultHttpResponse) {
            DefaultHttpResponse response = (DefaultHttpResponse) msg;
            long len = response.headers().getLong(HttpHeaderNames.CONTENT_LENGTH, 0L);
            isResource = len > 0 ? true : false;//文档由于采用的是chunked因此其长度为0，资源均为有长度
        }
        super.encode(ctx, msg, out);
    }

    @Override
    protected ZlibWrapper determineWrapper(CharSequence acceptEncoding) {
		if(isResource){
			return ZlibWrapper.NONE;//不压缩
		}
        return super.determineWrapper(acceptEncoding);
    }
}