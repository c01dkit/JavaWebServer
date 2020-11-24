import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ClientConnection extends Thread{
    private InputStream requestStream;
    private OutputStream responseStream;
    private final String serverRoot;
    private String fileName;
    private static final int MAX_BUFFER_LENGTH = 8192;
    private static final String account = "3180102943";
    private static final String password = "2943";
    private final byte[] responseBuffer = new byte[MAX_BUFFER_LENGTH];
    ClientConnection(Socket client, String deployPath) {
        serverRoot = deployPath; //address convert
        try{
            requestStream = client.getInputStream();
            responseStream = client.getOutputStream();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            RequestParser request = new RequestParser(requestStream);
            fileName = request.getFilePath();
            //ico ignored.
            if (request.getFileType().equals("ico")) return;
            if (request.getMethod().equals("GET")){
                LogPrinter.threadInfo("GET",request.getSource());
                handleGET(request);
            }
            if (request.getMethod().equals("POST")){
                LogPrinter.threadInfo("POST",request.getSource());
                handlePOST(request);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePOST(RequestParser request) throws IOException {
        String src = request.getSource();
        src = src.substring(src.lastIndexOf('/')+1);
        if (src.equals("dopost")){ //200
            Map<String, String> reqMap = request.getRequestMap();
            String login = reqMap.get("login");
            String pass = reqMap.get("pass");
            String msg;
            if (login.equals(account) && pass.equals(password)){
                msg = "Login Success!";
            } else {
                msg = "Login Failed.";
            }
            msg = "<html><body>" + msg + "</body></html>";
            setResponseHeaders(200,msg.getBytes().length,request);
            responseStream.write(msg.getBytes());
            responseStream.flush();
            responseStream.close();
        } else { //404
            setResponseHeaders(404,0,request);
        }
    }

    private void handleGET(RequestParser request) throws IOException, ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        File file = new File(serverRoot+unescape(fileName));
        if (file.exists()){ //200
            int len;
            InputStream fileInput= new FileInputStream(file);
            String rawData = null;
            if (request.getFileType().equals("zup")){
                rawData = file2String(file);
                if (rawData != null){
                    while (rawData.contains("<%") && rawData.contains("%>")){
                        int index1 = rawData.indexOf("<%");
                        int index2 = rawData.indexOf("%>");
                        String rep =string2eval(rawData.substring(index1+2,index2)).toString();
                        rawData = rawData.replace(rawData.substring(index1,index2+2),rep);
                    }
                    setResponseHeaders(200,rawData.length(),request);
                } else {
                    setResponseHeaders(200,file.length(),request);
                }
            } else if (request.getFileType().equals("jzup")){
                String path = serverRoot+request.getFileRoot();
                String noSuffix = request.getFileName().replace(".jzup","");
                File cwd = new File(path);
                String[] list = cwd.list();
                boolean compiled = false;
                if (list == null) {
                    setResponseHeaders(404,0,request);
                    return;
                }
                //Check whether this file has been visited before
                for (String i:list){
                    if (i.equals(noSuffix+"_jzup.class")){
                        compiled = true;
                        break;
                    }
                }
                if (!compiled){
                    String newFileName = serverRoot+request.getFileRoot()+"/"+noSuffix+"_jzup.java";
                    fileCopy(serverRoot+request.getSource(),newFileName);
                    compile(newFileName);
                }
            } else {
                setResponseHeaders(200,file.length(),request);
            }

            if (request.getFileType().equals("zup")){
                if (rawData != null) {
                    responseStream.write(rawData.getBytes());
                }else {
                    while ((len = fileInput.read(responseBuffer)) != -1) {
                        responseStream.write(responseBuffer, 0, len);
                    }
                }
            } else if (request.getFileType().equals("jzup")){
                String noSuffix = request.getFileName().replace(".jzup","");
                MyClassLoader myClassLoader = new MyClassLoader(serverRoot+request.getFileRoot());
                Class <?> cls = myClassLoader.findClass(noSuffix+"_jzup");
                Method method = cls.getMethod("Test");
                String value = (String)method.invoke(null);
                setResponseHeaders(200,value.getBytes().length,request);
                responseStream.write(value.getBytes());
            } else {
                while ((len = fileInput.read(responseBuffer)) != -1) {
                    responseStream.write(responseBuffer, 0, len);
                }
            }

            responseStream.flush();
            responseStream.close();
        } else { //404
            setResponseHeaders(404,0,request);
        }
    }

    private void setResponseHeaders(int stateCode, long fileLength,
                                    RequestParser request) throws IOException {
        String contentType = "Content-Type: text/html; charset=utf-8\r\n";
        if (request.getMethod().equals("GET")){
            switch (request.getFileType()){
                case "txt":
                case "java":
                case "zup":
                case "jzup":
                    contentType = "Content-Type: text/plain; charset=utf-8\r\n";
                    break;
                case "htm":
                case "html":
                    contentType = "Content-Type: text/html; charset=utf-8\r\n";
                    break;
                case "jpg":
                    contentType = "Content-Type: image/jpeg\r\n";
                    break;
                case "png":
                    contentType = "Content-Type: image/png\r\n";
                    break;
                case "gif":
                    contentType = "Content-Type: image/gif\r\n";
                    break;
                default:
                    contentType = "Content-Type: application/octet-stream\r\n";
            }
        }

        switch (stateCode){
            case 200:
                responseStream.write("HTTP/1.1 200 OK\r\n".getBytes());
                responseStream.write(contentType.getBytes());
                responseStream.write("Content-Length: ".getBytes());
                responseStream.write(String.valueOf(fileLength).getBytes());
                responseStream.write("\r\n\r\n".getBytes());
                break;
            case 404:
                responseStream.write("HTTP/1.1 404 file not found\r\n".getBytes());
                responseStream.write("Content-Type: text/html; charset=utf-8\r\n".getBytes());
                responseStream.write("Content-Length:22\r\n\r\n".getBytes());
                responseStream.write("<h1>404 Not Found</h1>".getBytes());
        }
    }

    private static String unescape(String s) {
        StringBuilder sbuf = new StringBuilder();
        int l = s.length();
        int ch = -1;
        int b, sumb = 0;
        for (int i = 0, more = -1; i < l; i++) {
            /* Get next byte b from URL segment s */
            switch (ch = s.charAt(i)) {
                case '%':
                    ch = s.charAt(++i);
                    int hb = (Character.isDigit((char) ch) ? ch - '0'
                            : 10 + Character.toLowerCase((char) ch) - 'a') & 0xF;
                    ch = s.charAt(++i);
                    int lb = (Character.isDigit((char) ch) ? ch - '0'
                            : 10 + Character.toLowerCase((char) ch) - 'a') & 0xF;
                    b = (hb << 4) | lb;
                    break;
                case '+':
                    b = ' ';
                    break;
                default:
                    b = ch;
            }
            /* Decode byte b as UTF-8, sumb collects incomplete chars */
            if ((b & 0xc0) == 0x80) { // 10xxxxxx (continuation byte)
                sumb = (sumb << 6) | (b & 0x3f); // Add 6 bits to sumb
                if (--more == 0)
                    sbuf.append((char) sumb); // Add char to sbuf
            } else if ((b & 0x80) == 0x00) { // 0xxxxxxx (yields 7 bits)
                sbuf.append((char) b); // Store in sbuf
            } else if ((b & 0xe0) == 0xc0) { // 110xxxxx (yields 5 bits)
                sumb = b & 0x1f;
                more = 1; // Expect 1 more byte
            } else if ((b & 0xf0) == 0xe0) { // 1110xxxx (yields 4 bits)
                sumb = b & 0x0f;
                more = 2; // Expect 2 more bytes
            } else if ((b & 0xf8) == 0xf0) { // 11110xxx (yields 3 bits)
                sumb = b & 0x07;
                more = 3; // Expect 3 more bytes
            } else if ((b & 0xfc) == 0xf8) { // 111110xx (yields 2 bits)
                sumb = b & 0x03;
                more = 4; // Expect 4 more bytes
            } else /*if ((b & 0xfe) == 0xfc)*/{ // 1111110x (yields 1 bit)
                sumb = b & 0x01;
                more = 5; // Expect 5 more bytes
            }
            /* We don't test if the UTF-8 encoding is well-formed */
        }
        return sbuf.toString();
    }
    private String file2String(File file) {
        InputStreamReader reader = null;
        StringWriter writer = new StringWriter();
        try {
            reader = new InputStreamReader(new FileInputStream(file));
            char[] buffer = new char[MAX_BUFFER_LENGTH];
            int n;
            while (-1 != (n = reader.read(buffer))) {
                writer.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return writer.toString();
    }
    private static boolean string2File(String res, String filePath) {
        boolean flag = true;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        try {
            File distFile = new File(filePath);
            if (!distFile.getParentFile().exists()) distFile.getParentFile().mkdirs();
            bufferedReader = new BufferedReader(new StringReader(res));
            bufferedWriter = new BufferedWriter(new FileWriter(distFile));
            char[] buf = new char[MAX_BUFFER_LENGTH];
            int len;
            while ((len = bufferedReader.read(buf)) != -1) {
                bufferedWriter.write(buf, 0, len);
            }
            bufferedWriter.flush();
            bufferedReader.close();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            flag = false;
            return flag;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    private static Object string2eval(String exec) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        Object result = null;
        try {
            result = engine.eval(exec);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void compile(String file2compile){
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null,null,null);
        Iterable units = fileManager.getJavaFileObjects(file2compile);
        JavaCompiler.CompilationTask t = compiler.getTask(null,null,null,null,null,units);
        t.call();
        try {
            fileManager.close();
            Files.delete(Paths.get(file2compile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fileCopy(String source, String dest) {
        InputStream in;
        OutputStream out;
        try {
            in = new FileInputStream(new File(source));
            out = new FileOutputStream(new File(dest));
            byte[] buffer = new byte[MAX_BUFFER_LENGTH];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
           e.printStackTrace();
        }
    }
}
