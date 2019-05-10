/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author luisb
 */
public class TransfereCC {
    
    private static int tam_buf = 200;
    
    /**
     * AgenteUDP que trata da comunicação.
     */
    private AgenteUDP agente;
    private int SYNC_NUM = 0; 
    private int SYNC_NUM_PROX =  0;
    private int ACK_NUM = 0;
    private int WINDOW_SIZE = 0;
    private int INITIAL_SEGMENT = 0;
    private int timeout = 72000; // 72 segundos
    
    /* 
    *   Inicialização de algumas variáveis associadas ao TransfereCC 
    */
    public TransfereCC() throws SocketException{
        Thread listener =   new TransfereCC_Manager();
        
        listener.start();
        
        this.agente = new AgenteUDP();
        this.agente.setTimeOut(timeout);
    }
   
    /* 
    *   Função responsável pelo início da transferência, onde, de acordo com o tipo de transferência 
    *   em questão, envia um sinal de controlo, ou seja o SYN de modo a inicializar a transferência 
    *   de dados, sendo que nesse SYN contém informação acerca do ficheiro, e do destino. De seguida, 
    *   consoante o tipo de transferência, é também direcionado para a função responsável por tratar
    *   do mecanismo por detrás dessa transferência. 
    *   @param transferType é o tipo de transferência, podendo ser um get ou um put. 
    *   @param filename o nome do ficheiro a ser transferido. 
    *   @param dest é o enderço ip destino. 
    *   @return void.
    */ 
    public void iniciaTransferencia(String transferType, String filename, InetAddress dest) throws Exception{
        
        Shared_Functions sf = new Shared_Functions();
        Packet ack = new Packet();
        
        if(transferType.equals("ccget")){
            
            LocalDateTime begin = LocalDateTime.now();
            
            SYNC_NUM = sf.sendSyn(this.agente, filename, false, dest);
            
            SYNC_NUM_PROX = SYNC_NUM;
                    
            try{
                ack = this.agente.receive(); // recebe o syn+ack
                this.ACK_NUM = ack.getSyncNum();
            
                LocalDateTime end = LocalDateTime.now();
                
                
            }
            catch(SocketTimeoutException ex){
                throw new Exception("A conexão expirou.");
            }
            
            if((new String(ack.getData())).equals("naoexiste")){
                throw new FileNotFoundException("O ficheiro não existe.");
            }
            
            downloadControl(filename, dest);
        }
        else {
           
            File f = new File(System.getProperty("user.home") + File.separator + 
                          "Files" + File.separator + 
                          "Input" + File.separator + 
                          filename);
        
            if(!f.exists()){
                throw new FileNotFoundException("O ficheiro não existe.");
            }
            
            SYNC_NUM = sf.sendSyn(this.agente, filename, true, dest);
            SYNC_NUM_PROX = SYNC_NUM;
            
            try{
                ack = this.agente.receive();
                
                this.ACK_NUM = ack.getSyncNum();
            }
            catch(Exception ex){
                throw new Exception("A conexão expirou.");
            }
                
            uploadControl(f, dest);
        }  
    }
   

    /* 
    *   Função cujo intuito é tratar dos mecanismos por detrás do GET de um ficheiro, onde 
    *   É aberta um RandomAcessFile, com o intuito de esctita, num path pré-definido, e enquanto 
    *   que é verdade, ou seja, num ciclo infinito abre um pacote conv que está sempre a espera 
    *   de receber pacotes, onde caso receba um pacote com a FIN flag a true (término de conecção), 
    *   sai do ciclo, caso contrário, calcula o checksum aqui no destino, e compara com o checksum 
    *   que vem no pacote, caso seja igual, não houve manipulação dos dados pelo caminho pelo que é 
    *   seguro escrever no ficheiro. Manda também um ACK para confirmar que recebeu a informação. 
    *   @param filename Nome do ficheiro. 
    *   @param dest Endereço IP destino. 
    *   @return void.
    */
    public void downloadControl(String filename, InetAddress dest) throws FileNotFoundException, IOException{
        
        String path = System.getProperty("user.home") + File.separator + "Files" + File.separator + "Output";
        
        RandomAccessFile downloadFile = new RandomAccessFile(path + File.separator + filename, "rw");
        Packet ack = new Packet();
        Packet conv = new Packet();
        long check = 0;
               
        Shared_Functions sf = new Shared_Functions();
        
        while(true){
            
            conv = agente.receive();
            
            ack = new Packet();
            
            if(conv.isAckFlag() && conv.getAckNum() != SYNC_NUM){
                ack.setAckFlag(true);
                ack.setAckNum(ACK_NUM);
                this.agente.send(ack.toString());
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
                System.out.println("Ack : " + ACK_NUM);
                ack.setAckNum(ACK_NUM);    
                // MANDA O ACK
                this.agente.send(ack.toString(),dest);
            } 
            else { 
                   // Mecanismo de retransmissão -> manda o mesmo ACK para saber que tem de retransmitir.
                   ack.setAckFlag(true);
                   ack.setAckNum(ACK_NUM); 
                   this.agente.send(ack.toString()); 
                   System.out.println("Reenvio do pacote!");
            }
            
        }   
  
        downloadFile.close();
        this.agente.setDestPort(7777);
        this.ACK_NUM = 0;
        this.SYNC_NUM = 0;
        this.SYNC_NUM = 0;
    }
     
    /*
    *   Função responsável por tratar do mecanismo do PUT na perspetiva da origem, onde novamente 
    *   é aberto um escritor para um path pré definido, e enquanto que o tamanho do ficheiro seja maior 
    *   que zero, o ficheiro é lido, e é transformado num array de bytes, de seguida é calculado o checksum 
    *   desse array de bytes, e é calculado também o payload (length) dos dados, sendo que são adicionados ao 
    *   pacote de seguida todos estes dados, tal como os dados a ser transferidos, o tamanho dos dados, 
    *   o checksum e o número de sequência. Para além disto, é também calculado aqui o próximo número 
    *   de sequência, e é recebido um pacote do tipo ACK, onde caso o número do ACK corresponda com o próximo
    *   número de sequência, é atualizado o número de sequência. No final é tratado o término da conecção 
    *   ao utilizar a função previamente explicada (sendFin). 
    *   @param filename Nome do ficheiro. 
    *   @param dest Endereço IP destino. 
    *   @return void.
    */
    public void uploadControl(File f, InetAddress dest) throws FileNotFoundException, IOException{
        
        RandomAccessFile uploadFile = new RandomAccessFile(f, "r");
        
        int n = 0; 
        int length = 0;
        long check = 0;
        Packet sendPacket = new Packet();
        Packet ack = new Packet(); 
        
        Shared_Functions sf = new Shared_Functions();
        
        long size = uploadFile.getChannel().size();
        
            while(size > 0){
                byte[] sendData;
                sendPacket = new Packet();
                
                if(size > tam_buf) {sendData = new byte[tam_buf]; n = uploadFile.read(sendData, 0, tam_buf);}     
                else {
                    sendData = new byte[(int) size]; 
                    n = uploadFile.read(sendData, 0, (int) size);
                }
                
                size = size - n; 
                
                check = sf.CRC32checksumByteArray(sendData);
                length = sendData.length;
                
                sendPacket.setData(sendData);                   // carregar o pacote com os dados
                sendPacket.setLengthData(length);               // carregar o pacote com o tamanho dos dados
                sendPacket.setChecksum(check);                  // carregar o checksum no pacote relativo ao PUT
                sendPacket.setSyncNum(SYNC_NUM);
                sendPacket.setAckFlag(true);
                sendPacket.setAckNum(ACK_NUM);
                
                SYNC_NUM_PROX += length;  
                
                do{ 
                    ack = new Packet();
                    this.agente.send(sendPacket.toString(), dest); 
                    ack = agente.receive();
                }while (ack.getAckNum() != SYNC_NUM_PROX);
                
                SYNC_NUM = SYNC_NUM_PROX;
            }
            
            uploadFile.close();
            
            sf.sendFin(this.agente,dest); // terminar a conecção
            this.agente.setDestPort(7777);
            this.ACK_NUM = 0;
            this.SYNC_NUM = 0;
            this.SYNC_NUM = 0;
    }
        
}







