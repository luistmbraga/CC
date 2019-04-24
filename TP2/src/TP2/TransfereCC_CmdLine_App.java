/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author luisb
 */
public class TransfereCC_CmdLine_App {
    
    
    public static void main(String args[]){
        
        // buffer para processar comandos dados pelo cliente
        BufferedReader inFromUser =
            new BufferedReader(new InputStreamReader(System.in));
        
        // string que irá conter o comando escrito
        String sentence = null;
        
        TransfereCC trans;
        try {
            trans = new TransfereCC();
        } catch (SocketException ex) {
            System.err.println("Erro ao iniciar o TransfereCC" + ex.getMessage());
            return;
        }
        
        String[] argumentos;
        
        do{
            // definição da interface
            System.out.print("TransfereCC > ");
            
            try { // ler o que o utilizador escreveu
                sentence = inFromUser.readLine(); 
            } catch (IOException ex) {
                System.out.println("Ocorreu um erro na leitura do comando");
            }
            System.out.println(sentence);
            
            // separa as várias partes do argumento em strings diferentes
            argumentos = sentence.split(" ");

            /*
            Inicio da interpretacao dos comandos
            */
            
            // menu de ajuda
            if(argumentos[0].equals("h") || argumentos[0].equals("help")){
                
                System.out.println(
                  "************************* HELP *************************\n"
                + "*                                                      *\n"
                + "*    SYNOPSIS:                                         *\n"
                + "*             ccget <filename>                         *\n"
                + "*             ccput <filename>                         *\n"
                + "*                                                      *\n"
                + "*    COMMANDS:                                         *\n"
                + "*             ccget - used to download a file          *\n"
                + "*             ccput - used to upload a file            *\n" 
                + "*                                                      *\n"
                + "*        In <filename> should be specified the name    *\n"
                + "*    of the file that you want to download or          *\n"
                + "*    upload.                                           *\n"
                + "*                                                      *\n"
                + "********************************************************");
            }
            
            if(argumentos[0].equals("ccget") || 
                    argumentos[0].equals("ccput")){
                
                InetAddress dest;
                try {
                    dest = InetAddress.getByName(argumentos[2]);
                } catch (UnknownHostException ex) {
                    System.err.println("Host Unknown " + ex.getMessage());
                    return;
                }
                
                try {
                    trans.iniciaTransferencia(argumentos[0], argumentos[1], dest);
                } catch (IOException ex) {
                    System.err.println("Erro de conecção " + ex.getMessage());
                }
            }
            else System.err.println("Comando não reconhecido");
            
            /*
            Fim da interpretacao dos comandos
            */
            
            
            // se se escrever exit sairemos do aplicação
        }while(!argumentos[0].equals("exit"));
        
    }
}
