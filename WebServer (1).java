

/**
 * WebServer Class
 * 
 * Implements a multi-threaded web server
 * supporting non-persistent connections.
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.logging.*;


public class WebServer extends Thread {

    int Wport;
    int Wtimeout;

	
	// global logger object, configures in the driver class
	private static final Logger logger = Logger.getLogger("WebServer");
	
	
    /**
     * Constructor to initialize the web server
     * 
     * @param port 	    The server port at which the web server listens > 1024
     * @param timeout 	The timeout value for detecting non-resposive clients, in milli-second units
     * 
     */
	public WebServer(int port, int timeout){
        Wport=port;
        Wtimeout=timeout;

    }



	
    /**
	 * Main web server method.
	 * The web server remains in listening mode 
	 * and accepts connection requests from clients 
	 * until the shutdown method is called.
	 *
     */
    private boolean shutdown = false;
	public void run(){
        ServerSocket serverSocket;
        ExecutorService executor=Executors.newFixedThreadPool(8);


        try {
            serverSocket=  new ServerSocket(Wport);
            serverSocket.setSoTimeout(100);
            
            // set timeout option below.
            while(!shutdown) {
                try{
                    Socket socket = serverSocket.accept();
                    System.out.println("Client Information: ");
                    System.out.println("------------------------------");
                    InetAddress address= socket.getInetAddress();
                    int port =socket.getPort();
                    System.out.println("IPAddress: "+address.toString().substring(1));
                    System.out.println("Port: "+port);
                    
                     // Connected to client
                    Server clientHandler = new Server(socket,Wtimeout);
                    //Thread thread = new Thread(clientHandler);
                    //thread.start();
                    executor.execute(clientHandler);
                    


                }
                catch(SocketTimeoutException e1){
                    

                }
                
            }


            if(shutdown==true){
                try{

                    executor.shutdownNow();

                    if(!executor.awaitTermination(5 , TimeUnit.SECONDS)){
                        executor.shutdownNow();
                    }


                }
                catch (InterruptedException e2){

                    executor.shutdownNow();
                    

                }

                serverSocket.close();




            }


            


    





        }

        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }
	

    /**
     * Signals the web server to shutdown.
	 *
     */
	public void shutdown(){

        shutdown=true;

    }
	
}




class Server implements Runnable {
    private final Socket socket;
    int timeout;
    public Server(Socket socket, int Wtimeout) {
        timeout = Wtimeout;
        this.socket = socket;
    }


    @Override
    public void run() {

        InputStream inSocket;
        OutputStream outSocket;
        try {
            String s;
            
            Date date= new Date();
            
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MM yyyy hh:mm:ss zzz");
            String strDate = formatter.format(date);
            strDate = formatter.format(date); 

            int readBytes=0;
            StringBuilder request= new StringBuilder();
            inSocket =socket.getInputStream();
            outSocket = socket.getOutputStream();
            socket.setSoTimeout(timeout);
            String check;

            while ((readBytes = inSocket.read() )!= -1) {

                request=request.append((char)readBytes);
                

                if(request.toString().contains("\r\n\r\n")){
                    break;
                }
                




            }
            System.out.println(request.toString());
            

            String lines[]=request.toString().split("\r\n");

            String requestLine=lines[0];

            String[] words=requestLine.split(" ");

            StringBuilder filePath = new StringBuilder();
            filePath.append(words[1]);
            

            if(filePath.toString() == "/"){
                filePath.append("index.html");
            }
            
            
            if(filePath.charAt(0)=='/'  ){

                filePath.deleteCharAt(0);


            }
            File temp = new File(filePath.toString());
            boolean exist=temp.exists();

            long timeModified=temp.lastModified();
            DateFormat mod=formatter;
            String dateMod = mod.format(timeModified);
            Path path = Paths.get(filePath.toString());



            if(words.length != 3 || !(words[0].equals("GET") && words[1].contains("/")&& words[2].equals("HTTP/1.1"))){

                

                StringBuilder responseBadRequest = new StringBuilder();
                responseBadRequest.append("HTTP/1.1 400 Bad Request\r\n");
                responseBadRequest.append("Date: ");
                responseBadRequest.append(strDate);
                responseBadRequest.append("\r\n");
                responseBadRequest.append("Server: Kazmi\r\n");
                responseBadRequest.append("Connection: close\r\n\r\n");

                String badRequest1 = responseBadRequest.toString();

                byte[] badBytes1=badRequest1.getBytes("US-ASCII");
                System.out.println(responseBadRequest.toString());
                outSocket.write(badBytes1);
                outSocket.flush();

                cleanup(inSocket,outSocket);
                
            }
            else if(exist == false){ 

                StringBuilder responseExistRequest = new StringBuilder();
                responseExistRequest.append("HTTP/1.1 404 Not Found\r\n");
                responseExistRequest.append("Date: ");
                responseExistRequest.append(strDate);
                responseExistRequest.append("\r\n");
                responseExistRequest.append("Server: Kazmi\r\n");
                responseExistRequest.append("Connection: close\r\n\r\n");

                String badRequest2 = responseExistRequest.toString();

                byte[] badBytes2=badRequest2.getBytes("US-ASCII");
                System.out.println(responseExistRequest.toString());
                outSocket.write(badBytes2);
                outSocket.flush();

                cleanup(inSocket,outSocket);


            }


            else{

                long lastModifiedLong=temp.lastModified();
                String lastModified= Long.toString(lastModifiedLong);

                long fileSizeLong=(long)temp.length();
                String fileSize= Long.toString(fileSizeLong);

                String fileType=Files.probeContentType(path);




                StringBuilder responseOKRequest = new StringBuilder();
                responseOKRequest.append("HTTP/1.1 200 OK\r\n");
                responseOKRequest.append("Date: ");
                responseOKRequest.append(strDate);
                responseOKRequest.append("\r\n");
                responseOKRequest.append("Server: Kazmi\r\n");
                responseOKRequest.append("Last-Modified: ");
                responseOKRequest.append(dateMod);
                responseOKRequest.append("\r\n");
                responseOKRequest.append("Content-Length: ");
                responseOKRequest.append(fileSize);
                responseOKRequest.append("\r\n");
                responseOKRequest.append("Content-Type: ");
                responseOKRequest.append(fileType);
                responseOKRequest.append("\r\n");
                responseOKRequest.append("Connection: close\r\n\r\n");

                String badRequest3 = responseOKRequest.toString();

                byte[] badBytes3=badRequest3.getBytes("US-ASCII");
                System.out.println(responseOKRequest.toString());
                
                outSocket.write(badBytes3);
                FileInputStream inFile;

                inFile = new FileInputStream(filePath.toString());

                byte[] arr = new byte[(int)filePath.length()];
                int bytesRead;
                while((bytesRead=inFile.read(arr,0,(int)filePath.length()))!=-1){
                    //outSocket.write(badBytes3,0,);
                    outSocket.write(arr,0,bytesRead);
                    outSocket.flush();

                }
                
                


                
                //outSocket.flush();

                cleanup(inSocket,outSocket);
                inFile.close();



            }




        }
        
        catch (SocketTimeoutException e){
            Date dateE= new Date();
            InputStream inSocketE;
            OutputStream outSocketE;
            
            try {
                outSocketE = socket.getOutputStream();
                inSocketE =socket.getInputStream();
                SimpleDateFormat formatterE = new SimpleDateFormat("EEE, dd MM yyyy hh:mm:ss zzz");
                String strDateE = formatterE.format(dateE);
                strDateE = formatterE.format(dateE); 

                StringBuilder responseTimeRequest = new StringBuilder();
                responseTimeRequest.append("HTTP/1.1 408 Request Timeout\r\n");
                responseTimeRequest.append("Date: ");
                responseTimeRequest.append(strDateE);
                responseTimeRequest.append("\r\n");
                responseTimeRequest.append("Server: Kazmi\r\n");
                responseTimeRequest.append("Connection: close\r\n\r\n");

                String badRequest4 = responseTimeRequest.toString();
                System.out.println(responseTimeRequest.toString());
                byte[] badBytes4;

                badBytes4 = badRequest4.getBytes("US-ASCII");
                outSocketE.write(badBytes4);
                outSocketE.flush();
                cleanup(inSocketE,outSocketE);
    
            } catch (IOException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }

            
                
                


        }
        
        
        catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        





    }

    public void cleanup(InputStream in, OutputStream out) {

        try {
            in.close();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

    }






}
