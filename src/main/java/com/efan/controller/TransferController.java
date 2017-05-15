package com.efan.controller;

import com.efan.model.TransferModel;
import com.efan.utils.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xuenianxiang on 2017/5/15.
 */

@RestController
@RequestMapping("/transfer")
public class TransferController {


    private static final String TRANSFERS_PAY = "https://api.mch.weixin.qq.com/mmpaymkttransfers/promotion/transfers"; // 企业付款

    private static final String APP_ID = ConfigUtil.getProperty("wx.appid");

    private static final String MCH_ID = ConfigUtil.getProperty("wx.mchid");

    private static final String API_SECRET = ConfigUtil.getProperty("wx.api.secret");


    //企业向个人支付转账
    @ApiOperation(value = "企业向个人支付转账")
    @RequestMapping(value = "/pay", method = RequestMethod.POST)
    public ModelMap transferPay(@RequestBody TransferModel model) {

        ModelMap result = new ModelMap();

        Map<String, String> restmap = null;
        try {
            Map<String, String> parm = new HashMap<String, String>();
            parm.put("mch_appid", APP_ID); //公众账号appid
            parm.put("mchid", MCH_ID); //商户号
            parm.put("nonce_str", PayUtil.getNonceStr()); //随机字符串
            parm.put("partner_trade_no", PayUtil.getTransferNo()); //商户订单号
            parm.put("openid", model.getOpenid()); //用户openid
            parm.put("check_name", "NO_CHECK"); //校验用户姓名选项 OPTION_CHECK
            parm.put("amount", model.getAmount()); //转账金额
            parm.put("desc", "测试转账到个人"); //企业付款描述信息
            parm.put("spbill_create_ip", getV4IP()); //Ip地址
            parm.put("sign", PayUtil.getSign(parm, API_SECRET));

            String restxml = HttpUtils.posts(TRANSFERS_PAY, XmlUtil.xmlFormat(parm, false));
            restmap = XmlUtil.xmlParse(restxml);
        } catch (Exception e) {

            result.put("result","0");
            result.put("message","转账失败");
            result.put("data",null);
        }

        if (CollectionUtil.isNotEmpty(restmap) && "SUCCESS".equals(restmap.get("result_code"))) {
            Map<String, String> transferMap = new HashMap<>();
            transferMap.put("partner_trade_no", restmap.get("partner_trade_no"));//商户转账订单号
            transferMap.put("payment_no", restmap.get("payment_no")); //微信订单号
            transferMap.put("payment_time", restmap.get("payment_time")); //微信支付成功时间

            result.put("result","1");
            result.put("message","转账成功");
            result.put("data",transferMap);

        }else {
            result.put("result","0");
            result.put("message","转账失败");
            result.put("data",restmap);
        }

        return result;
    }

    //获取公网ip
    public static String getV4IP(){
        String ip = "";
        String chinaz = "http://ip.chinaz.com";

        StringBuilder inputLine = new StringBuilder();
        String read = "";
        URL url = null;
        HttpURLConnection urlConnection = null;
        BufferedReader in = null;
        try {
            url = new URL(chinaz);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream(),"UTF-8"));
            while((read=in.readLine())!=null){
                inputLine.append(read+"\r\n");
            }
            //System.out.println(inputLine.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(in!=null){
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        Pattern p = Pattern.compile("\\<dd class\\=\"fz24\">(.*?)\\<\\/dd>");
        Matcher m = p.matcher(inputLine.toString());
        if(m.find()){
            String ipstr = m.group(1);
            ip = ipstr;
        }
        return ip;
    }
}
