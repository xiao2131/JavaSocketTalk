package com.example.demo;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;

public class TalkServer {
	// 保存聊天线程：
	private Map<String, ServerThread> map = new HashMap<String, ServerThread>();
	
    private static FtpServerFactory serverFactory = new FtpServerFactory();
    
    FTPClient ftp=new FTPClient();

	// private

	public static void main(String[] args) throws Exception {
		// 启动服务器
		new TalkServer().start();
	}

	/**
	 * 
	 * @Title: start
	 * @Description: 为每一个客户端创建一个独立线程
	 * @return:void
	 */
	public void start() throws Exception {
		// 服务器servesocket
		ServerSocket serverSocket = null;
		// 服务器socket
		Socket socket = null;
		
		// 启动FTP服务器
	    BaseUser user = new BaseUser();
	    user.setName("test");
	    user.setPassword("123456");
	    user.setHomeDirectory("E:\\ftpServer");
	    serverFactory.getUserManager().save(user);
	    FtpServer server = serverFactory.createServer();
	    server.start();
	    System.out.println("FTP服务器启动成功");

		try {
			// 服务器创建
			serverSocket = new ServerSocket(13163);
			// 死循环，用于接收更多的客户端
			while (true) {
				// 聊天线程创建
				socket = serverSocket.accept();
				// 为每一个客户端建立一个服务线程。
				ServerThread st = new ServerThread(socket);
				new Thread(st).start();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// 释放资源
			try {
				if (serverSocket != null) {
					serverSocket.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @ClassName: ServerThread
	 * @Description: 聊天线程
	 *
	 */
	private class ServerThread implements Runnable {
		
		

		private String cpk=null;

		// 相应的socket
		private Socket socket = null;
		private BufferedReader in = null;
		private PrintWriter out = null;
		// 每个聊天的名字
		private String name = null;
		// 聊天头
		private String meshead = null;
		// 停止线程标志位
		private boolean flag = true;
		// 单聊标志位：
		private boolean sin = false;

		/**
		 * 
		 * <p>
		 * Title:线程构造器
		 * </p>
		 * <p>
		 * Description:用来创建线程，以及初始化，和将线程加入map
		 * </p>
		 * 
		 * @param socket
		 * @throws IOException
		 */
		public ServerThread(Socket socket) throws Exception {

			// 获得socket
			this.socket = socket;
			// 获得输出和输入
			out = new PrintWriter(socket.getOutputStream(), true);
			// 获得用户名字
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			name = in.readLine();
			System.out.println("[服务器]："+name + "上线了");
			// 制作聊天头
			meshead = name + "[" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + "]";

//			KeyPair keyPair = RSAUtil.getKeyPair();
//			String privateKey = new String(Base64.encodeBase64(keyPair.getPrivate().getEncoded()));
//			String publicKey = new String(Base64.encodeBase64(keyPair.getPublic().getEncoded()));
//			userKeyPair.setPrivateKey(privateKey);
//			userKeyPair.setPublicKey(publicKey);

			// 将线程加入map：key为名字；value为线程
			map.put(name, this);
			out.println("[服务器发的消息]："+name + "已上线");

			// 提醒所有用户
//			send(meshead + "上线了");
		}

		/**
		 * 
		 * @Title: send
		 * @Description: 发送消息（群发）
		 * @param msg
		 */
		public void send(String msg) {
			// 迭代群发
			for (ServerThread thread : map.values()) {
				thread.out.println(msg);
			}
		}

		/**
		 * 
		 * @Title: Receiver
		 * @Description: 接收消息，并转发
		 * @return:void
		 */
		public void Receiver() throws Exception {
			String str = null;
			ServerThread qq = null;
			// 接收消息
			while ((str = in.readLine()) != null) {
				
				//判断是否是cpk
				if(str.indexOf("CPK#")>=0) {
					this.cpk=str.split("CPK#")[1];
				}

				// 如果消息为quit则退出
				if ("quit".equalsIgnoreCase(str)) {
					stop();
					// 给客户端发送关闭链接命令
					out.println("disconnect");
					break;
				}
				// 判断是否为单聊
				if (sin == false) {
					if (str.equals("single")) {
						// 如果为单聊就获得想要跟谁聊
						if ((str = in.readLine()) != null) {
							// 获得另外一个客户端线程
							qq = map.get(str);
							if (qq != null) {
								sin = true;
								out.println("OCPK#"+qq.cpk);
							}

						}
					} else {
						// 转发消息 群发
//						send(meshead + str);
					}

				}
				// 如果是单聊就给对应的客户端发送消息；
				else {

					// 如果为q则退出单聊
					if (str.equals("q")) {
						sin = false;
						qq.out.println(name + "对话结束");
					}
					// 否则发送消息
					else {
						qq.out.println(name + "对你说|" + str);
					}
				}

			}
		}

		/**
		 * 
		 * @Title: stop
		 * @Description: 停止函数
		 */
		public void stop() {
			flag = false;
			// 下线移去码map;
			map.remove(name);
			send(meshead + "已经下线了");
		}

		/**
		 * run方法
		 */
		@Override
		public void run() {

			try {
				while (flag) {
					// 不停接收消息
					Receiver();
				}
			} catch (SocketException e) {
				stop();// 客户端直接关闭引发的错误；
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				// 释放资源
				try {
					if (socket != null) {
						socket.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}
