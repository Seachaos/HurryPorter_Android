package com.seachaos.hurryporter;

import org.apache.http.NameValuePair;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by seachaos on 2/29/16.
 */
class HttpWand {
    private static String Charset = HurryPorter.Charset;

    private ArrayList<NameValue>
            headerData = new ArrayList<NameValue>(),
            postData = new ArrayList<NameValue>(),
            cookieData = new ArrayList<NameValue>();
    private ArrayList<fileInfo> files = new ArrayList<fileInfo>();

//    private String end = "\r\n";
//    private String twoHyphens = "--";
//    private String boundary = "*****";
    private HttpURLConnection connection;
    private String responseCookie;

    class NameValue {
        private String _name,_value;
        public NameValue(String name, String value){
            this._name = name;
            this._value = value;
        }
        public String getName(){
            return this._name;
        }
        public String getValue(){
            return this._value;
        }
    }

    // for upload files
    class fileInfo{
        public String fileName;
        public String inputName;
        public String reallyFilePath;
    }
    public static class CookieHelper{
        ArrayList<String> cookieName = new ArrayList<String>();
        ArrayList<String> cookieValue = new ArrayList<String>();
        public void setCookie(HttpWand wand){
            for(int ax=0;ax<cookieName.size();ax++){
                wand.addCookie(cookieName.get(ax), cookieValue.get(ax));
            }
        }
        public void getCookie(HttpWand wand){
            if(wand.responseCookie!=null){
                String[] responses = wand.responseCookie.split("\n");
                for(String response:responses){
                    parseCookie(response.split(";")[0]);
                }
            }
        }

        private void parseCookie(String s) {
            String[] tmp = s.split("=");
            if(tmp[0]!=null&&tmp[1]!=null){
                _setCookie(tmp[0], tmp[1]);
            }
        }
        private void _setCookie(String name, String value) {
            // check old cookie
            for(int ax=0;ax<cookieName.size();ax++){
                if(cookieName.get(ax).equals(name)){
                    cookieValue.set(ax, value);
                    return;
                }
            }
            cookieName.add(name);
            cookieValue.add(value);
        }
    }

    private String urlencode(String value){
        try {
            return URLEncoder.encode(value, Charset);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    public void addPost(String name, String value){
        name = urlencode(name);
        value = urlencode(value);
        postData.add(new NameValue(name, value));
    }
    public void addPost(String name, int value){
        addPost(name, Integer.toString(value));
    }
    public void addHeader(String name, String value){
        headerData.add(new NameValue(name, value));
    }

    public void addCookie(String name,String value){
        name = urlencode(name);
        value = urlencode(value);
        cookieData.add(new NameValue(name, value));
    }

    private HttpURLConnection _initURLConnection(String sendURL) throws IOException {
        URL url = new URL(sendURL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("POST");
        con.setConnectTimeout(10 * 1000);
        return con;
    }

    private void prepareHeaders(){
        addHeader("Charset", HurryPorter.Charset);
        addHeader("Content-Type", "application/x-www-form-urlencoded");
//        addHeader("Content-Type", "charset=utf-8;multipart/form-data;boundary=" + boundary);
    }


    private void setCookies(){
        String cookie = "";
        for(NameValue pd : cookieData){
            if(cookie.length()>0){
                cookie = cookie + ";"+pd.getName()+"="+pd.getValue();
            }else{
                cookie = pd.getName()+"="+pd.getValue();
            }
        }
        if(cookie.length()>0) {
            connection.setRequestProperty("Cookie", cookie);
        }
    }

    private void setHeaders(){
        for(int ax=0;ax<headerData.size();ax++){
            NameValue data = headerData.get(ax);
            connection.setRequestProperty(data.getName(), data.getValue());
        }
    }

    private void _closeOutputStream(DataOutputStream outputStream) throws IOException{
//        outputStream.writeBytes(twoHyphens+boundary+twoHyphens+end);
        // close stream
        outputStream.flush();
        outputStream.close();
    }
    

    public String send(String url){
        try {
            connection = _initURLConnection(url);
            if(connection==null){
                return null;
            }
            // prepare datas
            prepareHeaders();


            setHeaders();
            setCookies();

            // make connection
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            // write data to server
            _writePostValue(outputStream);

            // close for response
            _closeOutputStream(outputStream);

            // check headers
            if(connection.getHeaderField(0)==null) {
                return null;
            }
            if(connection.getHeaderField(0).indexOf("301")>0){
                // url 301
                return null;
            }
        }catch (Exception e) {
            return HurryPorter.ERROR_TAG + e.toString();
        }

        ByteArrayOutputStream byteArrayOutputStream = getServerByteArrayResponse();
        if(byteArrayOutputStream==null){
            return null;
        }
        // convert string
        String utf8 = null;
        try {
            utf8 = new String(byteArrayOutputStream.toByteArray(),Charset).trim();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return utf8;
    }

    private void _writePostValue(DataOutputStream outputStream) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
        writer.write(getQueryString());
        writer.flush();
        writer.close();
    }

    private String getQueryString() throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(int ax=0;ax<postData.size();ax++) {
            NameValue pair = postData.get(ax);
            if (first)
                first = false;
            else
                result.append("&");

            result.append(pair.getName());
            result.append("=");
            result.append(pair.getValue());
        }

        return result.toString();
    }

    private ByteArrayOutputStream getServerByteArrayResponse(){
        try{
            // get cookie return
            responseCookie = connection.getHeaderField("Set-Cookie");

            int totalLength = connection.getContentLength();
            InputStream is = connection.getInputStream();
            if(is==null) {
                return null;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte data[] = new byte[1024];
            int length = 0, getPer = 0;
            while((getPer = is.read(data))!=-1){
                length+=getPer;
                byteArrayOutputStream.write(data, 0, getPer);
            }

            // finish all connection
            is.close();
            byteArrayOutputStream.close();
            connection.disconnect();

            return byteArrayOutputStream;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
