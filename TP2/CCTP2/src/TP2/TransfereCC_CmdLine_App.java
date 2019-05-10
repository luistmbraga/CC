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
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author luisb
 */
public class TransfereCC_CmdLine_App {
    
    private static void imprimeHelp(){
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
    
    
    public static void main(String args[]){
        
        File inputFile = new File(System.getProperty("user.home") + File.separator + "Files" + File.separator + "Input" + File.separator);
        inputFile.mkdirs();
        File outputFile = new File(System.getProperty("user.home") + File.separator + "Files" + File.separator + "Output" + File.separator);
        outputFile.mkdirs();
        
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
                System.out.println("Ocorreu um erro na leitura do comando.");
            }
            
            // separa as várias partes do argumento em strings diferentes
            argumentos = sentence.split(" ");

            /*
            Inicio da interpretacao dos comandos
            */
            
            // menu de ajuda
            if(argumentos[0].equals("h") || argumentos[0].equals("help")){
                imprimeHelp();
            }
            else{
                if(argumentos[0].equals("ccget") || argumentos[0].equals("ccput")){
                
                    InetAddress dest;
                    try {
                        dest = InetAddress.getByName(argumentos[2]);
                    } catch (Exception ex) {
                        System.err.println("Host Unknown " + ex.getMessage());
                        return;
                    }

                    try {
                        System.out.println("'"+argumentos[1]+"'");
                        trans.iniciaTransferencia(argumentos[0], argumentos[1], dest);
                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                    }
                }
                else System.err.println("Comando não reconhecido. Comando h ou help para mais informações.");
            }
           
            /*
            Fim da interpretacao dos comandos
            */
                
            // se se escrever exit sairemos do aplicação
        }while(!argumentos[0].equals("exit"));
        
    }
}
