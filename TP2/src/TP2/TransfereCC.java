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
        Thread listener =   new Thread(man);
        
        listener.start();
        
        this.agente = new AgenteUDP();
        
    }
    
    /* 
    *   Cálculo do CRC32 (cyclic redundacy check) de um array de bytes, de modo
    *   a poder efetuar a validação do PDU recebido no destino.
    *   1ºlugar -> Calcula o checksum dos dados na origem; 
    *   2ºlugar -> Adiciona o checksum calculado anteriormente ao 
    *   pacote de dados; 
    *   3ºlugar -> Calcula o checksum novamente no destino, e compara esse valor 
    *   com o valor que veio no pacote de dados.
    *   @param bytes Array de bytes sobre o qual irá ser calculado o checksum 
    *   @return long Resultado do checksum do array de bytes
    */
    public long CRC32checksumByteArray(byte[] bytes){ 
        
        Checksum checksum = new CRC32();
         
        // update do checksum com o array de bytes passado como argumento
        checksum.update(bytes, 0, bytes.length);
          
        // retorna o valor em long do checksum do array de bytes
        long checksumValue = checksum.getValue(); 
        
        return checksumValue;
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
    *   Estabelece a conecção inicial com um envio de um SYN, onde é criado um novo pacote com 
    *   este propósito, sendo que a flag que representa o SYN é posta a true, é também gerado 
    *   um número aleatória para o número de sequência, sendo este depois adicionado ao pacote, 
    *   onde é também passado dados, de seguida o pacote é enviado para o endereço IP destino. 
    *   @param filename Nome do ficheiro.
    *   @param isWrite Indica se está para escrita ou não. 
    *   @param dest Endereço ip destino. 
    *   @return void.
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
    *   Responsável por enviar o sinal do término de conecção, onde, de maneira análoga à 
    *   função anterior, é criado um novo pacote, onde é colocada a flag do FIN a true, 
    *   sendo também gerado um número de sequência (embora não sendo necessário), sendo depois
    *   colocados dados aleatórios, uma vez que não interessa o tipo de dados a ser tratados 
    *   neste término ou início de conecção. 
    *   @param dest Endereço IP destino. 
    *   @return void.
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
                
                // DOWNLOAD MANDA ACK GET
                ACK_NUM += ACK_NUM + conv.getData().length; 
                conv.setAckNum(ACK_NUM);  
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
        
        long size = uploadFile.getChannel().size();
            while(size > 0){
                byte[] sendData;
                
                
                if(size > tam_buf) {sendData = new byte[tam_buf]; n = uploadFile.read(sendData, 0, tam_buf); ;}     
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
                sendPacket.setSyncNum(SYNC_NUM);
                
                System.out.println("CHECK ANTES DO PUT: " + check);
                
                SYNC_NUM_PROX = SYNC_NUM + sendPacket.getData().length; 
               
                this.agente.send(sendPacket.toString(), dest); 
                sendPacket = new Packet(); 
                
                ack = this.agente.receive(); 
                if (ack.getAckNum() == SYNC_NUM_PROX){                    
                    SYNC_NUM = SYNC_NUM_PROX;           // recebeu direito 
                }               
            }
            
            uploadFile.close();
            
            sendFin(dest);                                      // terminar a conecção
    }
   
    
    
    public class TransfereCC_Manager implements Runnable{
        
        private long check = 0;
        private int MAX_BUFFER_SIZE = 512;
        private AgenteUDP agente_request;
        private byte[] buffer = new byte[MAX_BUFFER_SIZE];
        public TransfereCC_Manager(){}
        
        
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
                    
                    ACK_NUM += ACK_NUM + received.getData().length; 
                    ack.setAckNum(ACK_NUM);                    
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
                
                if(size > tam_buf) {sendData = new byte[tam_buf]; n = uploadFile.read(sendData, 0, tam_buf);}
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
           
            uploadFile.close();
            
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
        
        /* 
        *   Runnable interface, de modo a tirar vantagem das threads, é aberto um 
        *   port no 7777 por pré definição, existindo um ciclo infrinito, onde 
        *   são recebidos os pacotes de dados, e caso o pacote de dados recebido 
        *   não possua uma fin flag a true, caso o apcote seja para escrita, 
        *   é retirado o nome do ficheiro do pacote, sendo este de seguida 
        *   processado e passado para o mecanismo do GET. Caso não seja para escrita
        *   é retirado novamente o nome do ficheiro, é é passado para o mecanismo PUT. 
        *   @return void.
        */
        public void run(){

            try {
                    this.agente_request = new AgenteUDP(7777);
                } catch (SocketException ex) {
                    System.err.println(ex.getMessage());
                    return;
                }
            
            while(true){
                Packet receive_datapacket;

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







