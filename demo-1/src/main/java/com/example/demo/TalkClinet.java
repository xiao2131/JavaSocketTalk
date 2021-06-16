package com.example.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.KeyPair;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * 
 * @ClassName: TalkClinet
 * @Description: 聊天客户端：
 *
 */
public class TalkClinet {

	private UserKeyPair userKeyPair;
	private String ocpk = null;
	// 获得控制台输入
	private BufferedReader sysin = null;
	// socket
	private Socket s = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	// 线程停止flag
	private boolean flag = true;
	private int count = 1;
	// 单聊标志
	private final static String single = "single";
	// 连接ftp服务器
	private FTPClient ftp = new FTPClient();

	public static void main(String[] args) throws Exception {
		// 启动客户端：
		new TalkClinet().start();
	}

	/**
	 * 
	 * @Title: start
	 * @Description: 主要作用：启动发送和接收任务
	 * @return:void
	 * @throws Exception
	 */
	public void start() throws Exception {
		try {
			KeyPair keyPair = RSAUtil.getKeyPair();
			String privateKey = new String(Base64.encodeBase64(keyPair.getPrivate().getEncoded()));
			String publicKey = new String(Base64.encodeBase64(keyPair.getPublic().getEncoded()));
			this.userKeyPair = new UserKeyPair();
			userKeyPair.setPrivateKey(privateKey);
			userKeyPair.setPublicKey(publicKey);

			//ftp登录
			int reply;
			ftp.connect("localhost", 21);
			ftp.login("test", "123456");
			reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				System.out.println("FTP连接失败: Fail to code" + reply);
			}

			// 获得系统输入：
			sysin = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
			// 设置名字：
			System.out.println("请输入您的用户名：");
			String name = sysin.readLine();
			// 建立客户端socket
			s = new Socket("127.0.0.1", 13163);
			// 获得socket输出out
			out = new PrintWriter(s.getOutputStream(), true);
			// 发送名字给服务器
			out.println(name);
			// 获得socket输入 in
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			// 建立接收线程；原因获取系统输入会阻塞主线程
			new Thread(new ClinetThread()).start();

			// 发送自己的公钥给服务器,让服务器存储
			out.println("CPK#" + publicKey);

			// 发送消息============================================================
			String str = null;
			System.err.println("[提示消息]：私聊请回复single，查看FTP服务器目录输入ls");
			while (flag && ((str = sysin.readLine()) != null)) {
				// 判断是否为单聊标志。如果不是单聊，就群发消息
				if (single.equalsIgnoreCase(str)) {
					System.out.println("请输入想跟谁聊 ：");
					// 获取系统输入：
					if (flag && ((str = sysin.readLine()) != null)) {
						// 发送单聊标志,这两个out.println如果发送,则是一起发送的
						out.println(single);
						// 发送要跟聊天的用户名
						out.println(str);
					}
				}
				// 显示服务器列表文件
				if (str.equalsIgnoreCase("ls")) {
					List<String> list = FtpUtil.getFileNameList(ftp);
					StringBuffer sb = new StringBuffer();
					for (String string : list) {
						sb.append(string + "\t");
					}
					System.out.println("==== FTP服务目录文件列表 ====");
					System.out.println(sb.toString());
					System.out.println("选择要下载的文件名");
					str = sysin.readLine();
					if (FtpUtil.downloadFile(ftp, str, "./")) {
						System.out.println(str + "下载成功");
					}else {
						System.out.println(str + "文件不存在");
					}
					continue;
				}

				if (ocpk != null) {
					// 向服务端发送加密内容：
					String encryptData = RSAUtil.encrypt(str, RSAUtil.getPublicKey(ocpk));
					out.println(encryptData);
				}

			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		finally {
			// 关闭资源
			if (sysin != null) {
				try {
					sysin.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private class ClinetThread implements Runnable {

		/**
		 * 
		 * @Title: recive
		 * @Description: 接收消息，当消息为disconnect时退出，此去需要在按下一次回车用来终止系统输入；
		 * @return:void
		 */
		private void recive() throws Exception {
			try {
				// 接收服务端消息
				String str = in.readLine();
				if (str != null) {
					// 如果是结束聊天，就退出线程
					if ("disconnect".equals(str)) {
						stop();
						System.out.println("回车退出:");
					} else {
						// 接受的是公钥
						if (str.indexOf("OCPK#") >= 0) {
							ocpk = str.split("OCPK#")[1];
							System.out.println("用户收到OCPK" + ocpk);
							return;
						}

						if (str.indexOf("对你说") >= 0) {
							String newStr[] = str.split("对你说\\|");
							String username = newStr[0];
							String message = newStr[1];
							System.out.println(username + "发送加密前的内容：" + message);
							String decryptData = RSAUtil.decrypt(message,
									RSAUtil.getPrivateKey(userKeyPair.getPrivateKey()));
							System.out.println("[" + username + "对你说]" + decryptData);
							return;

						}

						// 否则显示消息
						System.out.println(str);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// 线程停止方法
		public void stop() {
			flag = false;
		}

		// 线程主要任务
		@Override
		public void run() {
			while (flag) {
				// 接收消息函数调用
				try {
					recive();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
