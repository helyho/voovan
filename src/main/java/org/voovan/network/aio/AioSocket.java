package org.voovan.network.aio;

import org.voovan.Global;
import org.voovan.network.*;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.messagesplitter.TimeOutMesssageSplitter;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * AioSocket 连接
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AioSocket extends SocketContext {

	private AsynchronousSocketChannel	socketChannel;
	private AioSession					session;
	private EventTrigger				eventTrigger;
	private ReadCompletionHandler		readCompletionHandler;
	private ConnectedCompletionHandler	connectedCompletionHandler;

	/**
	 * 构造函数
	 * 
	 * @param host   主机地址
	 * @param port   主机端口
	 * @param readTimeout 主机超时时间
	 * @throws IOException IO 异常
	 */
	public AioSocket(String host, int port, int readTimeout) throws IOException {
		super(host, port, readTimeout);
		this.socketChannel = AsynchronousSocketChannel.open();
		session = new AioSession(this);
		eventTrigger = new EventTrigger(session);

		connectedCompletionHandler = new ConnectedCompletionHandler(eventTrigger);
		readCompletionHandler = new ReadCompletionHandler(eventTrigger, session.getByteBufferChannel());
		this.handler = new SynchronousHandler();
		connectModel = ConnectModel.CLIENT;
	}

	/**
	 * 构造函数
	 *
	 * @param parentSocketContext 父异步 socket 通道
	 * @param socketChannel 异步 socket 通道
	 *
	 * @throws IOException IO 异常
	 */
	protected AioSocket(SocketContext parentSocketContext, AsynchronousSocketChannel socketChannel) throws IOException {
		this.socketChannel = socketChannel;
		this.copyFrom(parentSocketContext);
		session = new AioSession(this);
		eventTrigger = new EventTrigger(session);

		connectedCompletionHandler = new ConnectedCompletionHandler(eventTrigger);
		readCompletionHandler = new ReadCompletionHandler(eventTrigger, session.getByteBufferChannel());
		connectModel = ConnectModel.SERVER;
	}

	/**
	 * 获取事件触发器
	 * 
	 * @return 事件触发器
	 */
	protected EventTrigger getEventTrigger() {
		return eventTrigger;
	}

	/**
	 * 捕获 Aio Connect
	 * @throws IOException  IO 异常
	 */
	protected void catchConnected() throws IOException {
		InetSocketAddress socketAddress = new InetSocketAddress(this.host, this.port);
		socketChannel.connect(socketAddress, this, connectedCompletionHandler);
		//获取到对端 IP 地址为连接成功
		while(socketChannel.getRemoteAddress()==null){
			TEnv.sleep(1);
		}
	}

	/**
	 * 捕获 Aio Read
	 * @param buffer 缓冲区
	 */
	protected void catchRead(ByteBuffer buffer) {
		if(socketChannel.isOpen()) {
			socketChannel.read(buffer, readTimeout, TimeUnit.MILLISECONDS, buffer, readCompletionHandler);
		}
	}

	/**
	 * 获取 Session 对象
	 * @return  Session 对象
     */
	public AioSession getSession() {
		return session;
	}

	/**
	 * 初始化 SSL 环境
	 * @throws SSLException SSL 异常
     */
	private void initSSL() throws SSLException{
		if (connectModel == ConnectModel.SERVER && sslManager != null) {
			sslManager.createServerSSLParser(session);
		} else if (connectModel == ConnectModel.CLIENT && sslManager != null) {
			sslManager.createClientSSLParser(session);
		}
	}
	
	@Override
	public void start() throws IOException{
		initSSL();
		
		//如果没有消息分割器默认使用读取超时时间作为分割器
		if(messageSplitter == null){
			messageSplitter = new TimeOutMesssageSplitter();
		}
		
		if (connectModel == ConnectModel.CLIENT) {
			// 捕获 connect 事件
			try {
				catchConnected();
			}catch (IOException e){
				return;
			}
		}
		
		//捕获输入事件
		catchRead(ByteBuffer.allocateDirect(1024));
		
		// 触发 connect 事件
		eventTrigger.fireConnectThread();

		Global.getThreadPool().execute( () -> {
			// 等待ServerSocketChannel关闭,结束进程
			while (isConnect() && (connectModel==ConnectModel.CLIENT || eventTrigger.isShutdown())) {
				TEnv.sleep(500);
			}
		});

	}

	/**
	 * 获取 SocketChannel 对象
	 * 
	 * @return 异步 Socket 通道
	 */
	public AsynchronousSocketChannel socketChannel() {
		return this.socketChannel;
	}

	@Override
	public boolean isConnect() {
		return socketChannel.isOpen();
	}

	/**
	 * 同步读取消息
	 * @return 读取出的对象
	 * @throws ReadMessageException  读取消息异常
	 */
	public Object synchronouRead() throws ReadMessageException {
		return session.synchronouRead();
	}

	/**
	 * 同步发送消息
	 * @param obj  要发送的对象
	 * @throws SendMessageException  消息发送异常
	 */
	public void synchronouSend(Object obj) throws SendMessageException {
		session.synchronouSend(obj);
	}

	@Override
	public boolean close() {
		if (socketChannel != null) {
			try {

				// 触发 DisConnect 事件
				eventTrigger.fireDisconnect();

				// 检查是否关闭线程池
				// 如果不是ServerSocket下的 Socket 则关闭线程池
				// ServerSocket下的 Socket由 ServerSocket来关闭线程池
				if (connectModel == ConnectModel.CLIENT) {
					eventTrigger.shutdown();
				}

				// 关闭 Socket 连接
				if (socketChannel.isOpen() && (connectModel == ConnectModel.SERVER || eventTrigger.isShutdown())) {
					socketChannel.close();
				}
				return true;
			} catch (IOException e) {
				Logger.error("SocketChannel close failed",e);
				return false;
			}
		} else {
			return true;
		}
	}

}
