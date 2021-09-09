package edu.escuelaing.arep.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HttpServer {
    private static HttpServer instance = new HttpServer();
    private HttpServer(){}
    public static HttpServer getInstance(){
        return instance;
    }
    public static void main(String[] args) throws IOException {
        HttpServer.getInstance().startServer(args);
    }
    public void startServer(String[] args) throws IOException {
        int port = getPort();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: "+port);
            System.exit(1);
        }
        Socket clientSocket = null;
        boolean running = true;
        while(running) {
            try {
                System.out.println("Listo para recibir en puerto "+port);
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
            processRequest(clientSocket);
        }
        serverSocket.close();
    }

    static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 5000; //returns default port if heroku-port isn't set(i.e. on localhost)
    }

    public void processRequest(Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String inputLine, outputLine;
        String method="";
        String path = "";
        String version = "";
        List<String> headers = new ArrayList<String>();
        while ((inputLine = in.readLine()) != null) {
            if (method.isEmpty()) {
                String[] requestStrings = inputLine.split(" ");
                method = requestStrings[0];
                path = requestStrings[1];
                version = requestStrings[2];
                System.out.println("reques: " + method + " " + path + " " + version);
            } else {
                System.out.println("header: " + inputLine);
                headers.add(inputLine);
            }
            System.out.println("Received: " + inputLine);
            if (!in.ready()) {
                break;
            }
        }
        String responseMessage = createResponse(path);
        out.println(responseMessage);
        out.close();
        in.close();
        clientSocket.close();
    }

    public String createResponse(String path){
        String type = "text/html";
        if(path.endsWith(".css")){
            type = "text/css";
        }else if(path.endsWith(".js")){
            type = "text/javascript ";
        }else if(path.endsWith(".jpeg")){
            type = "image/jpeg";
        }else if(path.endsWith(".png")){
            type = "image/png";
        }
        if (path.startsWith("/calculadora?operation")){
            String op = path.substring(23,26);
            String num = path.substring(34);
            System.out.println(operations(op,num));
            return "HTTP/1.1 200 OK \r\n"
                    + "Content-Type: "+type+"\r\n"
                    + "\r\n"
                    + operations(op,num);
        }
        else {
            Path file = Paths.get("./www" + path);
            Charset charset = Charset.forName("UTF-8");
            String outmsg = "";
            try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    outmsg += "\r\n" + line;
                }
            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
            }
            return "HTTP/1.1 200 OK \r\n"
                    + "Content-Type: "+type+"\r\n"
                    + "\r\n"
                    + outmsg;
        }
    }

    public static String operations(String op,String input){
        Double res = null;
        double pi = 1;
        String num = "";
        String den = "";
        System.out.println(op);
        if (input.contains("pi")){
            pi = Math.PI;
            input = "";
        }
        if(input.contains("/")){
            num = input.substring(0,input.indexOf("/"));
            den = input.substring(input.indexOf("/")+1);
            if(num.length() == 0){
                num = "1";
            }
            if(den.length() == 0){
                den = "1";
            }
        }
        else {
            den = "1";
            if (input.length()==0){
                num = "1";
            }else {
                num = input;
            }
        }
        double r = Integer.parseInt(num)*pi/Integer.parseInt(den);
        if(op == null){
            op = "cos";
        }
        switch (op){
            case "sin":
                System.out.println("Seno");
                res = Math.sin(r);
                break;
            case "cos":
                System.out.println("Coseno");
                res = Math.cos(r);
                break;
            case "tan":
                System.out.println("Tangente");
                res = Math.tan(r);
                break;
        }
        assert res != null;
        return "{ response: \""+res.toString()+"\"}";
    }
}

