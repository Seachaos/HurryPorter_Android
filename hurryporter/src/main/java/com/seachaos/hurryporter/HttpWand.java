package com.seachaos.hurryporter;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

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

    private String end = "\r\n";
    private String twoHyphens = "--";
    private String boundary = "*****";
    private HttpURLConnection connection;
    private String responseCookie;
    protected int httpStatusCode = 0;

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
        if(HurryPorter.beforePostUseURLEncode) {
            name = urlencode(name);
            value = urlencode(value);
        }
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
        cookieData.add(new NameValue(name,value));
    }

    public void appendFile(String inputName,String fileName,String filePath){
        fileInfo fe = new fileInfo();
        fe.inputName = inputName;
        fe.fileName = fileName;
        fe.reallyFilePath = filePath;
        files.add(fe);
    }

    private void _writePostValue(DataOutputStream outputStream) throws IOException{
        for(int ax=0;ax<postData.size();ax++){
            NameValue data = postData.get(ax);
            outputStream.writeBytes(twoHyphens+boundary+end);
            outputStream.writeBytes("Content-Disposition: form-data;");
            outputStream.writeBytes("name=\""+data.getName()+"\""+end+end);
            outputStream.write(data.getValue().getBytes("UTF-8"));
            outputStream.writeBytes(end);
        }
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
        addHeader("Content-Type", "multipart/form-data;boundary=" + boundary);
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

    private void _fileUpload(DataOutputStream outputStream) throws IOException{
        for(int ax=0;ax<files.size();ax++){
            fileInfo fe = files.get(ax);
            outputStream.writeBytes(twoHyphens+boundary+end);
            outputStream.writeBytes("Content-Disposition: form-data;");
            //Log.d("msg",fe.fileName);
            outputStream.writeBytes("name=\""+fe.inputName+"\";filename=\""+fe.fileName+"\""+end+end);
            int bufferSize = 1024;
            int length = -1;
            byte[] buffer = new byte[bufferSize];
            FileInputStream fStream = new FileInputStream(fe.reallyFilePath);
            while((length = fStream.read(buffer))!=-1){
                outputStream.write(buffer,0,length);
            }
            outputStream.writeBytes(end);
        }
    }

    private void _closeOutputStream(DataOutputStream outputStream) throws IOException{
        outputStream.writeBytes(twoHyphens+boundary+twoHyphens+end);
        // close stream
        outputStream.flush();
        outputStream.close();
    }


    public String send(String url){

//        SchemeRegistry schemeRegistry = new SchemeRegistry();
//        schemeRegistry.register(new Scheme("https", (SocketFactory) new NoSSLv3Factory(), 443));

        HttpPost httpRequest = new HttpPost(url);

        List<NameValuePair> params = new ArrayList<NameValuePair>();

        for(int ax=0;ax<postData.size();ax++){
            NameValue data = postData.get(ax);
            params.add(new BasicNameValuePair(data.getName(), data.getValue()));
        }

        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
//            client = new DefaultHttpClient();
            HttpClient client = TlsOnlySocketFactory.createHttpClient();
            HttpResponse httpResponse = client.execute(httpRequest);

            httpStatusCode = httpResponse.getStatusLine().getStatusCode();
            if (httpStatusCode == 200) {
                String strResult = EntityUtils.toString(httpResponse
                        .getEntity());
                return strResult;
            }
        }catch (Exception e){
            String error = e.toString();
            error = e.toString();
        }
        return null;
    }

    public String _send(String url){
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
            _fileUpload(outputStream);

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
