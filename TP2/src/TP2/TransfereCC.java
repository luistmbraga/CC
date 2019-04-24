/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luisb
 */
public class TransfereCC {
    
    /**
     * AgenteUDP que trata da comunicação.
     */
    private AgenteUDP agente;
    private int SYNC_NUM = 0;
    private int ACK_NUM = 0;
    private int WINDOW_SIZE = 0;
    private int INITIAL_SEGMENT = 0;
    private final String InputPath = "C:/Users/luisb/Desktop/CC/TP2/Input/";  // C:/Users/luisb/Desktop/CC/TP2/Input/teste.txt"
    private final String OutputPath = "C:/Users/luisb/Desktop/CC/TP2/Output/";
    private List<DatagramPacket> BUFFER = new ArrayList<DatagramPacket>();
    
    
    public TransfereCC() throws SocketException{
        TransfereCC_Manager man = new TransfereCC_Manager();
        Thread listener =   new Thread(man);
        
        listener.start();
        
        this.agente = new AgenteUDP();
        
    }
    
    public Packet convertToPacket(DatagramPacket p){
        return Packet.valueOf(new String(p.getData()));
    }
    
    public void iniciaTransferencia(String transferType, String filename, InetAddress dest) throws IOException{
        
        // handshake 1/3
        if(transferType.equals("ccget")) sendSyn(filename, false, dest);
        else sendSyn(filename, true, dest);
        
        System.out.println("Acabei de enviar o syn " + filename);
/*
        //handshake 2/3 
        Packet receivePacket = agente.receive();
        
        if(receivePacket.getAckNum() == SYNC_NUM + 1){
            Packet ackPacket = new Packet();
            SYNC_NUM = receivePacket.getAckNum();
            ackPacket.setSyncFlag(true);
            ackPacket.setSyncNum(SYNC_NUM);
            
            ACK_NUM = receivePacket.getSyncNum() + 1;
            ackPacket.setAckFlag(true);
            ackPacket.setAckNum(ACK_NUM);
            
            WINDOW_SIZE = 16;
            
            ackPacket.setWindowSize(WINDOW_SIZE);
            
            // handshake 3/3
            agente.send(ackPacket.toString(), dest);
            
            SYNC_NUM =  INITIAL_SEGMENT = ACK_NUM;
            SYNC_NUM--;
        }
        */
        
        // begin transfer
        if(transferType.equals("ccget")){
            downloadControl(filename, dest);
        }
        
        if(transferType.equals("ccput")){
            uploadControl(filename, dest);
        }
    }
    
    public void sendSyn(String filename, boolean isWrite, InetAddress dest) throws IOException{
        Packet syncPacket = new Packet();
        syncPacket.setSyncFlag(true);
        SYNC_NUM = ThreadLocalRandom.current().nextInt(1,5000);
        syncPacket.setSyncNum(SYNC_NUM);
        syncPacket.setData(filename);
        
        if(isWrite) syncPacket.setWRFlag(true);
        
        agente.send(syncPacket.toString(), dest);
    }

    /**
     * Função geral que controla uma transferência do tipo download.
     * 
     * -> Não tem contolo sobre ACKs duplicados
     * -> Não detecta buracos na repção de pacotes
     * 
     * -> Validação apenas devido ao numero de sequencia ser maior que o anterior,
     *  portanto não recebe pacotes fora de ordem
     * 
     * @param filename
     * @throws FileNotFoundException 
     */
    public void downloadControl(String filename, InetAddress dest) throws FileNotFoundException, IOException{
        
        RandomAccessFile downloadFile = new RandomAccessFile(OutputPath + filename, "rw");
        DatagramPacket received;
        Packet ack = new Packet();
        Packet conv = new Packet();
        
        
        while(true){
            System.out.println("Não recebi");
            received = agente.receive();
            System.out.println("Recebi !!!!!");
            ack = new Packet();
            conv = convertToPacket(received);
            
            
            //  Se o buffer estourar
            //if(BUFFER.size() == WINDOW_SIZE){
                
            //}
            
            // se a flag for de finalização  
            // fourway handshake 1/4
            //if(received.isFinFlag()){
              //  break;
            //}
            
            /*  se o numero de sequencia for maior que o anterior é válido
                !!!!no entanto não detecta buracos na chegada de pacotes!!!! (fora de ordem)    
            */
            //if(received.getSyncNum() > SYNC_NUM){
                // é adicionado ao buffer
                BUFFER.add(received);
                System.out.println("Estou a receber os dados");
                // tem de se enviar um ACK de volta
                //ack.setAckFlag(true);
                //ack.setAckNum(received.getSyncNum() + 1);
                //ack.setWindowSize(WINDOW_SIZE - BUFFER.size());
                
                // send ACK
                //agente.send(ack.toString(), dest);
                
              //  SYNC_NUM = INITIAL_SEGMENT + received.getData().length()-1;
            //}
            System.out.println(conv.getData());
            downloadFile.writeChars(conv.getData());
        }/*
        // fouway handshake 2/4
        ack = new Packet();
        
        ack.setAckFlag(true);
        ack.setAckNum(ACK_NUM);
        ack.setAckNum(received.getSyncNum() + 1);
        
        agente.send(ack.toString(), dest);
        
        // fourway handshake 3/4
        ack = new Packet();
        
        ack.setFinFlag(true);
        ack.setAckFlag(true);
        ack.setAckNum(received.getSyncNum() + 1);
        agente.send(ack.toString(), dest);
        
        // fourway handshake 4/4
        received = agente.receive();
        
        if(received.isAckFlag()){}
        
        // percorre todos os pacotes e construi o objecto*/
        
        
        //for(Packet p : BUFFER)
            
    }
    
    public void downloadControl(String filename) throws FileNotFoundException, IOException{
        
        
        RandomAccessFile downloadFile = new RandomAccessFile(OutputPath + filename, "rw");
        System.out.println("Foi aqui acima que fodeu");
        
        DatagramPacket received;
        Packet ack = new Packet();
        
        while(true){
            received = agente.receive();
            ack = new Packet();
            
            //  Se o buffer estourar
            //if(BUFFER.size() == WINDOW_SIZE){
                
            //}
            
            // se a flag for de finalização  
            // fourway handshake 1/4
            //if(received.isFinFlag()){
              //  break;
            //}
            
            /*  se o numero de sequencia for maior que o anterior é válido
                !!!!no entanto não detecta buracos na chegada de pacotes!!!! (fora de ordem)    
            */
            //if(received.getSyncNum() > SYNC_NUM){
                // é adicionado ao buffer
                BUFFER.add(received);
                // tem de se enviar um ACK de volta
                //ack.setAckFlag(true);
                //ack.setAckNum(received.getSyncNum() + 1);
                //ack.setWindowSize(WINDOW_SIZE - BUFFER.size());
                
                // send ACK
                //agente.send(ack.toString(), dest);
                
              //  SYNC_NUM = INITIAL_SEGMENT + received.getData().length()-1;
            //}
            downloadFile.write(received.getData());
        }/*
        // fouway handshake 2/4
        ack = new Packet();
        
        ack.setAckFlag(true);
        ack.setAckNum(ACK_NUM);
        ack.setAckNum(received.getSyncNum() + 1);
        
        agente.send(ack.toString(), dest);
        
        // fourway handshake 3/4
        ack = new Packet();
        
        ack.setFinFlag(true);
        ack.setAckFlag(true);
        ack.setAckNum(received.getSyncNum() + 1);
        agente.send(ack.toString(), dest);
        
        // fourway handshake 4/4
        received = agente.receive();
        
        if(received.isAckFlag()){}
        
        // percorre todos os pacotes e construi o objecto*/
        
        
        //for(Packet p : BUFFER)
            
    }
    
    public void uploadControl(String filename, InetAddress dest) throws FileNotFoundException, IOException{
        RandomAccessFile uploadFile = new RandomAccessFile(InputPath + filename, "r");
        
        int n = 0;
        byte[] sendData = new byte[512];
        Packet sendPacket = new Packet();
        
        while(n != -1){
            n = uploadFile.read(sendData);
            
            sendPacket.setData(sendData.toString());
            agente.send(sendPacket.toString(), dest);
        }
    }
   
    public class TransfereCC_Manager implements Runnable{
        
        private int MAX_BUFFER_SIZE = 512;
        private AgenteUDP agente_request;
        private byte[] buffer = new byte[MAX_BUFFER_SIZE];
        public TransfereCC_Manager(){}
         
        public void uploadControl(String filename) throws FileNotFoundException, IOException{
                
        System.out.println("Path actual: |" + InputPath + filename + "|");  // "C:/Users/luisb/Desktop/CC/TP2/Input/teste1.txt"
        try{
            String name = new String(filename);
            String e = InputPath + name;
            String str = InputPath + filename;
            File f = new File(str);
            System.out.println("Can read: " + f.canRead() + " Absolute path: " + f.getAbsolutePath());
            RandomAccessFile uploadFile = new RandomAccessFile("C:/Users/luisb/Desktop/CC/TP2/Input/teste1.txt", "r");
            System.out.println("Já abri o descritor de ficheiro ");
            int n = 0;
            byte[] sendData = new byte[512];
            Packet sendPacket = new Packet();

            while(n != -1){
                n = uploadFile.read(sendData);

                sendPacket.setData(sendData);
                this.agente_request.send(sendPacket.toString());
            }
        
        }
        catch( IOException e){
            e.printStackTrace();
            return;
        }
        
        System.out.println("Ja li");
    }
        
        public void run(){
              
            Packet receive_packet = new Packet();
            
            DatagramPacket receive_datapacket;
            
            try {
                this.agente_request = new AgenteUDP(7778);
            } catch (SocketException ex) {
                System.err.println(ex.getMessage());
                return;
            }
             
            
            try {
                receive_datapacket = this.agente_request.receive();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                return;
            }
            
            receive_packet =  convertToPacket(receive_datapacket);
            
            if(receive_packet.isWRFlag()){
                
                String filename = receive_packet.getData();
                try {
                    System.out.println("Download " + filename);
                    downloadControl(filename);
                } catch (IOException ex) {
                   System.err.println(ex.getMessage());
                }                              
            }
            else{
                String filename = receive_packet.getData();
                try {
                    System.out.println("Upload |" + filename + "|");
                    uploadControl(filename);
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
    
    
    
}







