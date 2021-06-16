package com.example.demo;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author yirs
 * 2021年6月16日20:30:42
 * 	测试实例
 */

public class TestDemo {

	//启动FTP服务器，启动成功后，浏览器访问ftp://localhost
	@Test
    public void createFtp2(){
        try {
            FtpServerFactory serverFactory = new FtpServerFactory();
            BaseUser user = new BaseUser();
            //为FTP创建用户名和密码
            user.setName("test");
            user.setPassword("123456");
            //ftp目录
            user.setHomeDirectory("E:\\ftpServer");
            serverFactory.getUserManager().save(user);
            FtpServer server = serverFactory.createServer();
            server.start();
            System.out.println("ftp搭建完成");      
            
            //程序结束，FTP服务器也关闭了
            while(true);
        }catch (
                FtpException e){
            e.printStackTrace();
        }
    }
	
	//测试下载文件
	@Test
	public void test() {
		System.out.println(FtpUtil.downloadFile("localhost", 21, "test", "123456", "/", "1.txt", "./"));
	}
	
	
	
	
	
}
