/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author luisb
 */
public class TransfereCC_ThreadRequest extends Thread {
    
    private static int tam_buf = 200;
      
    private int SYNC_NUM = 0; 
    private int SYNC_NUM_PROX =  0;
    private int ACK_NUM = 0;
    private int WINDOW_SIZE = 0;
    private int INITIAL_SEGMENT = 0;
    
    private AgenteUDP agente_request;
    
    public TransfereCC_ThreadRequest(int destPort, InetAddress destIp, String filename, boolean wr, int ack_num){
        this.destPort = destPort;
        this.destIp = destIp;
        this.filename = filename;
        this.wr = wr;
        this.ACK_NUM = ack_num;
        this.SYNC_NUM = ThreadLocalRandom.current().nextInt(1,5000);
        this.SYNC_NUM_PROX = this.SYNC_NUM;
    }
    
    /* 
        *   Função responsável pelo mecanismo do GET na perspetiva do destino, onde é aberto um escritor 
        *   de ficheiro para um path pré definido, sendo criadas as pastas caso ainda não as possua 
        *   criadas. De seguida, num loop infinito, são recebidos os pacotes, sendo calculado agora o checksum 
        *   dos dados recebidos, caso o checksum calculado aqui corresponda com o do pacote, os dados 
        *   são retirados do pacote recebidos, e são escritos no ficheiro desde a posição zero, até 
        *   ao próprio tamanho dos dados. De seguida é enviado novamente um ACK de confirmação. 
        *   @param filename Nome do ficheiro. 
        *   @return void. 
        */
        public void downloadControl(String filename) throws FileNotFoundException, IOException{
        String path = System.getProperty("user.home") + File.separator + "Files" + File.separator + "Output";
        
        RandomAccessFile downloadFile = new RandomAccessFile(path + File.separator + filename, "rw");
        Packet ack = new Packet();
        Packet conv = new Packet();
        long check = 0;
               
        Shared_Functions sf = new Shared_Functions();
        
        while(true){
            
            conv = agente_request.receive();
            ack = new Packet();
            
            if(conv.isAckFlag() && conv.getAckNum() != SYNC_NUM){
                ack.setAckFlag(true);
                ack.setAckNum(ACK_NUM);
                this.agente_request.send(ack.toString());
                System.out.println("Entrei aqui!!");
            }
            
            if(conv.isFinFlag()){ 
                break; 
            }
            
            check = sf.CRC32checksumByteArray(conv.getData()); 
            
            if (conv.getChecksum() == check ){
                //BUFFER.add(received);
                
                downloadFile.write(conv.getData());               
                
                // DOWNLOAD MANDA ACK GET
                ACK_NUM += conv.getData().length; 
                ack.setAckNum(ACK_NUM);
                // MANDA O ACK
                this.agente_request.send(ack.toString());
                
            } 
            else { 
                   // Mecanismo de retransmissão -> manda o mesmo ACK para saber que tem de retransmitir.
                   ack.setAckNum(ACK_NUM); 
                   this.agente_request.send(ack.toString()); 
                   System.out.println("Reenvio do pacote!");
            }
            
        }   
        downloadFile.close();    
    }
        
        /* 
        *   Responsável pelo PUT na perspetiva da origem, ou seja, é novamente aberto um escritor 
        *   de ficheiros num path pré definido, sendo calculado o tamanho do ficheiro, caso 
        *   o tamanho seja maior que zero, os dados são lidos e passados para um array de bytes 
        *   sendo este array de bytes depois passado para dentro do pacote a enviar, sendo calculado 
        *   também o payload (length) dos dados, a ser adicionados no pacote também, bem como o chekcsum 
        *   do array de bytes correspondente aos dados, onde tal como nos outros, é adicionado também 
        *   ao pacote de dados a enviar. por fim o pacote é enviado. No final de todo este processo, 
        *   é enviado um FIN com um mecanismo semelhante à função sendFin. 
        *   @param filename Nome do ficheiro. 
        *   @return void.
        */
        public void uploadControl(File f) throws FileNotFoundException, IOException{
                
        try{
            
            RandomAccessFile uploadFile = new RandomAccessFile(f, "r");
            
            long n = 0;
            
            long size = uploadFile.getChannel().size();
            int tam = 0;
            long check = 0; 
            Packet sendPacket = new Packet();
            Shared_Functions sf = new Shared_Functions();
            
            while(size > 0){
                byte[] sendData;
                sendPacket = new Packet();
                Packet ack;
                
                if(size > tam_buf) {
                    sendData = new byte[tam_buf];
                    n = uploadFile.read(sendData, 0, tam_buf);
                }
                else {
                    sendData = new byte[(int) size]; 
                    n = uploadFile.read(sendData, 0, (int) size);
                }
        
                size = size - n;
                
                check = sf.CRC32checksumByteArray(sendData);
                tam = sendData.length; 
                
                sendPacket.setData(sendData);
                sendPacket.setLengthData(tam); 
                sendPacket.setChecksum(check);
                sendPacket.setSyncNum(SYNC_NUM);
                sendPacket.setAckFlag(true);
                sendPacket.setAckNum(ACK_NUM);
                
                SYNC_NUM_PROX += tam;
                
                do{ 
                    ack = new Packet();
                    this.agente_request.send(sendPacket.toString());
                    ack = agente_request.receive();
                }while (!ack.isAckFlag() && ack.getAckNum() != SYNC_NUM_PROX);
                
                SYNC_NUM = SYNC_NUM_PROX;
            }
           
            uploadFile.close();
            
            sendPacket.setFinFlag(true);
            sendPacket.setLengthData(4);
            sendPacket.setData("null".getBytes());
            this.agente_request.send(sendPacket.toString()); 
        }
        catch( IOException e ){
            e.getMessage();
        }
    }
    
    private int destPort;
    private InetAddress destIp;
    private String filename;
    private boolean wr;
    
    public void run() {
        try {
            this.agente_request = new AgenteUDP();
        } catch (SocketException ex) {
            System.err.println(ex.getMessage());
        }
        
        this.agente_request.setDestIp(destIp);
        this.agente_request.setDestPort(destPort);
        
        Packet ack = new Packet();
        ack.setAckFlag(true);
        ack.setAckNum(ACK_NUM);
        ack.setSyncFlag(true);
        ack.setSyncNum(this.SYNC_NUM);
        
         if(wr){
                try {
                    this.agente_request.send(ack.toString()); // send SYN+ACK
                    downloadControl(filename);
                } catch (IOException ex) {
                   System.err.println(ex.getMessage());
                }                              
            }
            else{
                try {
                    File f = new File(System.getProperty("user.home") + File.separator + 
                              "Files" + File.separator + 
                              "Input" + File.separator + 
                              filename);
            
                    if(!f.exists()){
                        ack.setData("naoexiste".getBytes());
                        ack.setLengthData("naoexiste".length());
                        this.agente_request.send(ack.toString());
                        return;
                    }
                    
                    this.agente_request.send(ack.toString());
                    
                    uploadControl(f);
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            } 
    }
    
}
