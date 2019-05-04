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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

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
    private List<DatagramPacket> BUFFER = new ArrayList<DatagramPacket>(); 
        
    
    public TransfereCC() throws SocketException{
        TransfereCC_Manager man = new TransfereCC_Manager();
        Thread listener =   new Thread(man);
        
        listener.start();
        
        this.agente = new AgenteUDP();
        
    }
    
    /* 
        Cálculo do CRC32 (cyclic redundacy check) de um array de bytes.
    */
    public long CRC32checksumByteArray(byte[] bytes){ 
        
        Checksum checksum = new CRC32();
         
        // update do checksum com o array de bytes passado como argumento
        checksum.update(bytes, 0, bytes.length);
          
        // retorna o valor em long do checksum do array de bytes
        long checksumValue = checksum.getValue(); 
        
        return checksumValue;
    }
    
    
    public Packet convertToPacket(DatagramPacket p){
        return Packet.valueOf(new String(p.getData()));
    }
    
    private static String cleanTextContent(String text){
        text = text.replaceAll(null , "");
        
        return text;
    }
    
    public void iniciaTransferencia(String transferType, String filename, InetAddress dest) throws IOException{
        
        
        if(transferType.equals("ccget")) sendSyn(filename, false, dest);
        else sendSyn(filename, true, dest);
        
        System.out.println("Acabei de enviar o syn " + filename);

        if(transferType.equals("ccget")){
            downloadControl(filename, dest);
        }
        
        if(transferType.equals("ccput")){
            uploadControl(filename, dest);
        }
    }
    
    /*
        Função responsável por estabelecer a conecção inicial, envia o SYN inicial
    */
    public void sendSyn(String filename, boolean isWrite, InetAddress dest) throws IOException{
        Packet syncPacket = new Packet();
        syncPacket.setSyncFlag(true);
        SYNC_NUM = ThreadLocalRandom.current().nextInt(1,5000);
        syncPacket.setSyncNum(SYNC_NUM); 
        syncPacket.setLengthData(filename.getBytes().length);
        syncPacket.setData(filename.getBytes());
        
        if(isWrite) syncPacket.setWRFlag(true);
        
        agente.send(syncPacket.toString(), dest);
    } 
    
    /*
        Função responsável pelo término da conecção, onde envia o FIN.
    */
    public void sendFin(InetAddress dest) throws IOException{ 
        Packet finPacket = new Packet(); 
        finPacket.setFinFlag(true); 
        SYNC_NUM = ThreadLocalRandom.current().nextInt(1,5000); 
        finPacket.setSyncNum(SYNC_NUM); 
        finPacket.setLengthData(4);
        finPacket.setData("null".getBytes());
        
        agente.send(finPacket.toString(),dest); 
    }

    
    public void downloadControl(String filename, InetAddress dest) throws FileNotFoundException, IOException{
        
        String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CCTP2" + File.separator + "Fich" + File.separator + "Output";
        
        RandomAccessFile downloadFile = new RandomAccessFile(path + File.separator + filename, "rw");
        Packet ack = new Packet();
        Packet conv = new Packet();
        long check = 0;
               
        while(true){
            
            System.out.println("Não recebi");
            conv = agente.receive();
            System.out.println("Recebi !!!!!");
            ack = new Packet();
            
            if(conv.isFinFlag()) break;
            
            
            check = CRC32checksumByteArray(conv.getData()); 
            System.out.println("CHECK RECEBIDO: " + check + "  o que veio" + conv.getChecksum());
            
            
            if (conv.getChecksum() == check ){
                //BUFFER.add(received);
                System.out.println("Estou a receber os dados");
                          
                downloadFile.write(conv.getData());               
            }
        }    
  
    
    }
    
   
    
    
    public void uploadControl(String filename, InetAddress dest) throws FileNotFoundException, IOException{
        
        String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CCTP2" + File.separator + "Fich" + File.separator + "Input";
        RandomAccessFile uploadFile = new RandomAccessFile(path + File.separator + filename, "r");
        
        int n = 0; 
        int length = 0;
        long check = 0;
        Packet sendPacket = new Packet();
        
        long size = uploadFile.getChannel().size();
            while(size > 0){
                byte[] sendData;
                
                
                if(size > 454) {sendData = new byte[454]; n = uploadFile.read(sendData, 0, 454); ;}     
                else {
                    sendData = new byte[(int) size]; 
                    n = uploadFile.read(sendData, 0, (int) size);
                }
                
                size = size - n; 
                
                check = CRC32checksumByteArray(sendData); 
                System.out.println("Dados: " + check); 
                length = sendData.length;
                
                sendPacket.setData(sendData);                   // carregar o pacote com os dados
                sendPacket.setLengthData(length);               // carregar o pacote com o tamanho dos dados
                sendPacket.setChecksum(check);                  // carregar o checksum no pacote relativo ao PUT
                
                System.out.println("CHECK ANTES DO PUT: " + check);
                
                this.agente.send(sendPacket.toString(), dest);
            }
            
            sendFin(dest);                                      // terminar a conecção
    }
   
    
    
    public class TransfereCC_Manager implements Runnable{
        
        private long check = 0;
        private int MAX_BUFFER_SIZE = 512;
        private AgenteUDP agente_request;
        private byte[] buffer = new byte[MAX_BUFFER_SIZE];
        public TransfereCC_Manager(){}
        
        public void downloadControl(String filename) throws FileNotFoundException, IOException{
            
            long check = 0;
            
            String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CCTP2" + File.separator + "Fich" + File.separator + "Output";
            File f = new File(path);
            if(!f.exists())f.mkdirs();
            
            File f2 = new File(path+File.separator+filename);
            System.out.println(f2.getPath());
            
            if (f2.exists()) { System.out.println("O ficheiro já existe!"); }
            else{
                System.out.println("O ficheiro está a ser criado!");
                boolean createNewFile = f2.createNewFile();
                System.out.println("O ficheiro foi criado = " + createNewFile);
            }
          
            System.out.println(f2.getPath());
            System.out.println("Posso ler? " + f.canRead());
            RandomAccessFile downloadFile = new RandomAccessFile(f2 , "rw");
            System.out.println("Foi aqui acima que fodeu");

            Packet received;
            Packet ack = new Packet();

            while(true){
                received = agente_request.receive();
                ack = new Packet();

                //BUFFER.add(received);
                               
                //downloadFile.writeChars(new String(conv.getData()).replaceAll("\0", "")); 
                
                // calcula o checksum do pacote recebido após o put           
                check = CRC32checksumByteArray(received.getData());  
                
                if (received.isFinFlag()) break;
                
                if (received.getChecksum() == check){
                    
                    // retira o array de bytes do pacote que está a ler, começa na posição 0 e vai até ao length
                    downloadFile.write(received.getData(),0,received.getLengthData());                    
                }
            
            }
            
            
         }
        
        
        public void uploadControl(String filename) throws FileNotFoundException, IOException{
                
        try{
             String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CCTP2" + File.separator + "Fich" + File.separator + "Input"; 
             
            RandomAccessFile uploadFile = new RandomAccessFile(path + File.separator + filename, "r");
            
            System.out.println("Já abri o descritor de ficheiro ");
            long n = 0;
            Packet sendPacket = new Packet();
            long size = uploadFile.getChannel().size();
            int tam = 0;
            long check = 0;
            
            while(size > 0){
                byte[] sendData;
                
                if(size > 454) {sendData = new byte[454]; n = uploadFile.read(sendData, 0, 454); ;}
                else {
                    sendData = new byte[(int) size]; 
                    n = uploadFile.read(sendData, 0, (int) size);
                }
               
                
                size = size - n;
                sendPacket.setData(sendData);
                
                tam = sendPacket.getData().length; 
                sendPacket.setLengthData(tam); 
                
                check = CRC32checksumByteArray(sendPacket.getData()); 
                System.out.println("CHECK UPLOAD " + check);
                
                sendPacket.setChecksum(check); 
                System.out.println("Pacote possui check: " + sendPacket.getChecksum());
                 
                this.agente_request.send(sendPacket.toString());
                      
            }
           
            sendPacket.setFinFlag(true);  
            SYNC_NUM = ThreadLocalRandom.current().nextInt(1,5000); 
            sendPacket.setSyncNum(SYNC_NUM); 
            sendPacket.setLengthData(4);
            sendPacket.setData("null".getBytes());
            System.out.println("SEND PACKET FINAL: " + sendPacket.isFinFlag());
            this.agente_request.send(sendPacket.toString()); 
           
        }
        catch( IOException e ){
            e.printStackTrace();
            return;
        }
        
        System.out.println("Ja li");
    }
        
        public void run(){
            
            while(true){
                Packet receive_datapacket;

                try {
                    this.agente_request = new AgenteUDP(7777);
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

                if(!receive_datapacket.isFinFlag())
                    if(receive_datapacket.isWRFlag()){

                        String filename = new String(receive_datapacket.getData());
                        filename = filename.replaceAll("\0", ""); 
                        System.out.println(filename);

                        try {
                            System.out.println("Download " + filename);
                            downloadControl(filename);
                        } catch (IOException ex) {
                           System.err.println(ex.getMessage());
                        }                              
                    }
                    else{
                        String filename = new String(receive_datapacket.getData());
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
    
    
    
}







