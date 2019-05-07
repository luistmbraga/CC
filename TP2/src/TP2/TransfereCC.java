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
    private List<DatagramPacket> BUFFER = new ArrayList<DatagramPacket>(); 
        
    
    /* 
    *   Inicialização de algumas variáveis associadas ao TransfereCC 
    */
    public TransfereCC() throws SocketException{
        TransfereCC_Manager man = new TransfereCC_Manager();
        Thread listener =   new TransfereCC_Manager();
        
        listener.start();
        
        this.agente = new AgenteUDP();
        
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
    public void iniciaTransferencia(String transferType, String filename, InetAddress dest) throws IOException{
        
        Shared_Functions sf = new Shared_Functions();
                
        if(transferType.equals("ccget")) SYNC_NUM = sf.sendSyn(this.agente, filename, false, dest);        
        else SYNC_NUM = sf.sendSyn(this.agente, filename, true, dest);
        
        System.out.println("Acabei de enviar o syn " + filename);

        if(transferType.equals("ccget")){
            downloadControl(filename, dest);
        }
        
        if(transferType.equals("ccput")){
            uploadControl(filename, dest);
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
        
        String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CCTP2" + File.separator + "Fich" + File.separator + "Output";
        
        RandomAccessFile downloadFile = new RandomAccessFile(path + File.separator + filename, "rw");
        Packet ack = new Packet();
        Packet conv = new Packet();
        long check = 0;
               
        Shared_Functions sf = new Shared_Functions();
        
        while(true){
            
            System.out.println("Não recebi");
            conv = agente.receive();
            System.out.println("Recebi !!!!!");
            ack = new Packet();
            
            if(conv.isFinFlag()){ 
                System.out.println("o fdp do fin: " + conv.isFinFlag());
                break; 
            }
            
            
            check = sf.CRC32checksumByteArray(conv.getData()); 
            System.out.println("CHECK RECEBIDO: " + check + "  o que veio" + conv.getChecksum());
            
            
            if (conv.getChecksum() == check ){
                //BUFFER.add(received);
                System.out.println("Estou a receber os dados");
                          
                downloadFile.write(conv.getData());               
                
                // DOWNLOAD MANDA ACK GET
                ACK_NUM += conv.getData().length; 
                ack.setAckNum(ACK_NUM);    
                // MANDA O ACK
                this.agente.send(ack.toString(),dest);
                
                System.out.println("MANDOU ACK NO GET NA RECEÇÃO: " + ack.getAckNum());
            } 
            else { 
                   // Mecanismo de retransmissão -> manda o mesmo ACK para saber que tem de retransmitir.
                   ack.setAckNum(ACK_NUM); 
                   this.agente.send(ack.toString()); 
                   System.out.println("Reenvio do pacote!");
            }
            
        }   
  
        downloadFile.close();
        
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
    public void uploadControl(String filename, InetAddress dest) throws FileNotFoundException, IOException{
        
        String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "CCTP2" + File.separator + "Fich" + File.separator + "Input";
        RandomAccessFile uploadFile = new RandomAccessFile(path + File.separator + filename, "r");
        
        int n = 0; 
        int length = 0;
        long check = 0;
        Packet sendPacket = new Packet();
        Packet ack = new Packet(); 
        
        Shared_Functions sf = new Shared_Functions();
        
        long size = uploadFile.getChannel().size();
            while(size > 0){
                byte[] sendData;
                
                
                if(size > tam_buf) {sendData = new byte[tam_buf]; n = uploadFile.read(sendData, 0, tam_buf); ;}     
                else {
                    sendData = new byte[(int) size]; 
                    n = uploadFile.read(sendData, 0, (int) size);
                }
                
                size = size - n; 
                
                check = sf.CRC32checksumByteArray(sendData); 
                System.out.println("Dados: " + check); 
                length = sendData.length;
                
                sendPacket.setData(sendData);                   // carregar o pacote com os dados
                sendPacket.setLengthData(length);               // carregar o pacote com o tamanho dos dados
                sendPacket.setChecksum(check);                  // carregar o checksum no pacote relativo ao PUT
                sendPacket.setSyncNum(SYNC_NUM);
                
                System.out.println("CHECK ANTES DO PUT: " + check);
                
                SYNC_NUM_PROX += SYNC_NUM + sendPacket.getData().length; 
               
                this.agente.send(sendPacket.toString(), dest); 
                sendPacket = new Packet(); 
                
                ack = this.agente.receive(); 
                if (ack.getAckNum() == SYNC_NUM_PROX){                    
                    SYNC_NUM = SYNC_NUM_PROX;           // recebeu direito 
                }               
            }
            
            uploadFile.close();
            
            sf.sendFin(this.agente,dest);                                      // terminar a conecção
    }
        
}







