package com.example.test;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HttpControllet {

	@GetMapping("/jyu")
	@ResponseBody
	public String test() {
		String url = "https://www.jyu.edu.cn/sy.htm";
		String result = HttpUtils.getUrl(url);
		System.out.println(result);
		return result;
	}
	
	@RequestMapping("/*")
	public String hello() {
		return "/hello.htm";
	}
	
	@GetMapping("/login")
	public String login() {
		return "/login.htm";
	}
	
	@PostMapping("/verf")
	public String very(@RequestParam("username") String username,
            @RequestParam("password") String password) {
		System.out.println("用户名是"+username);
		System.out.println("密码是"+password);
		return "/hello.htm";
	}

}
