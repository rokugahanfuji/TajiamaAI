/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import gui.MessageRecevable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author koji
 */
public class ServerConnecter {
    private PrintWriter writer;
    private BufferedReader reader;
    private Socket connectedSocket;
    private ArrayList<MessageRecevable> recevers;
    
    public ServerConnecter(MessageRecevable rc){
        this.recevers = new ArrayList<MessageRecevable>();
        this.recevers.add(rc);
    }
    
    public void addMessageRecever(MessageRecevable rc){
        this.recevers.add(rc);
    }

    public void connectToServer(String hostname,int port) throws IOException{
            this.connectedSocket = new Socket(hostname,port);
            this.reader = new BufferedReader(new InputStreamReader(this.connectedSocket.getInputStream(),"UTF-8"));
            this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.connectedSocket.getOutputStream(),"UTF-8")));
            Runnable listenThread = new Runnable() {
                @Override
                public void run() {
                    String line;
                    try{
                        while((line = ServerConnecter.this.reader.readLine()) != null){
                            for(MessageRecevable rc:ServerConnecter.this.recevers){
                                rc.reciveMessage(line);
                            }
                        }
                   } catch (IOException ex) {
                        for(MessageRecevable rc:ServerConnecter.this.recevers){
                            rc.addMessage("IOException");
                        }
                   }
                }
            };
            Thread th = new Thread(listenThread);
            th.start();
    }
    
    public void sendMessage(String sendText) {
        if(this.writer != null){
            this.writer.println(sendText);
            this.writer.flush();
        }
    }

    public boolean canWrite() {
        return (this.writer != null);
    }

}
