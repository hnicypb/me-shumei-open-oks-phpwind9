package me.shumei.open.oks.phpwind9;

import java.io.IOException;
import java.util.HashMap;


import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.content.Context;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
	String resultFlag = "false";
	String resultStr = "未知错误！";
	
	/**
	 * <p><b>程序的签到入口</b></p>
	 * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
	 * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
	 * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
	 * @param cfg “配置”栏内输入的数据
	 * @param user 用户名
	 * @param pwd 解密后的明文密码
	 * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
	 */
	public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
		//把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
		CaptchaUtil.context = ctx;
		//标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
		CaptchaUtil.isAutoSign = isAutoSign;
		
		try{
			//存放Cookies的HashMap
			HashMap<String, String> cookies = new HashMap<String, String>();
			//Jsoup的Response
			Response res;
			
			//检查用户配置的URL，如果不以“http://”开头则帮用户补上
			//但在说明信息里面，还是需要跟用户明说要加“http://”，以免用户都不知道加还是不加的好
			String url = cfg.trim();
			if(url.startsWith("http://") == false && url.startsWith("https://") == false) {
				url = "http://" + url;
			}
			String loginPageUrl = url + "/index.php?m=u&c=login";//登录页面的URL
			String loginCheckUrl = url + "/index.php?m=u&c=login&a=checkname";//检查用户名的URL
			String loginSubmitUrl = url + "/index.php?m=u&c=login&a=dorun";//提交登录信息的URL
			
			String taskCenterUrl = url +"/index.php?m=task";//任务中心URL 
			String ssoUrl = "";//单点登录的URL
			String signUrl = url + "/index.php?m=space&c=punch&a=punch";//签到URL
			
			//访问登录页面
			res = Jsoup.connect(loginPageUrl).userAgent(UA_CHROME).method(Method.GET).timeout(TIME_OUT).ignoreContentType(true).execute();
			cookies.putAll(res.cookies());
			//提取CSRF验证串
			String csrf_token = cookies.get("csrf_token");
			
			//检查账号
			res = Jsoup.connect(loginCheckUrl).data("csrf_token", csrf_token).data("username", user).header("Accept", "application/json, text/javascript, */*; q=0.01").referrer(loginPageUrl).userAgent(UA_CHROME).cookies(cookies).method(Method.POST).timeout(TIME_OUT).ignoreContentType(true).execute();
			cookies.putAll(res.cookies());
			
			//提交账号密码
			res = Jsoup.connect(loginSubmitUrl)
					.data("username", user)
					.data("password", pwd)
					.data("backurl", taskCenterUrl)
					.data("invite", "")
					.data("csrf_token", csrf_token)
					.cookies(cookies)
					.userAgent(UA_CHROME)
					.timeout(TIME_OUT)
					.referrer(loginPageUrl)
					.ignoreContentType(true)
					.method(Method.POST)
					.execute();
			cookies.putAll(res.cookies());
			
			//获取单点登录URL并访问
			ssoUrl = res.parse().select(".error_return a").first().attr("href");
			res = Jsoup.connect(ssoUrl).userAgent(UA_CHROME).cookies(cookies).referrer(loginPageUrl).method(Method.GET).timeout(TIME_OUT).ignoreContentType(true).execute();
			cookies.putAll(res.cookies());
			
			//提交签到信息
			//{"state":"success","data":{"behaviornum":7,"reward":"2\u679a\u94dc\u5e01"}}
			res = Jsoup.connect(signUrl).data("csrf_token", csrf_token).userAgent(UA_CHROME).referrer(loginPageUrl).cookies(cookies).method(Method.POST).timeout(TIME_OUT).ignoreContentType(true).execute();
			System.out.println(res.body());
			
			try {
				JSONObject jsonObj = new JSONObject(res.body());
				String state = jsonObj.getString("state");
				if(state.equals("success"))
				{
					resultFlag = "true";
					JSONObject data = jsonObj.getJSONObject("data");
					int behaviornum = data.getInt("behaviornum");
					String reward = data.getString("reward");
					resultStr = "签到成功，获得" + reward + "，连续签到" + behaviornum + "天";
				}
			} catch (JSONException e) {
				//解析JSON出错，返回的是网页
				String J_html_error = res.parse().getElementById("J_html_error").text();
				if(J_html_error.contains("今天已经"))
				{
					resultFlag = "true";
					resultStr = "您今天已经打卡了哦，明天再来吧";
				}
				else
				{
					resultFlag = "false";
					resultStr = "登录成功，但提交签到请求时返回错误信息，签到失败";
				}
			}
			
			
		} catch (IOException e) {
			this.resultFlag = "false";
			this.resultStr = "连接超时";
			e.printStackTrace();
		} catch (Exception e) {
			this.resultFlag = "false";
			this.resultStr = "未知错误！";
			e.printStackTrace();
		}
		
		return new String[]{resultFlag, resultStr};
	}
	
	
}
