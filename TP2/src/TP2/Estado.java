/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TP2;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author luisb
 */
public class Estado {
    
    // nome do ficheiro, dados do ficheiro 
    private Map<String, FicheiroInfo> ficheiros;
    private int numEntradas;
   
    
    public Estado(){
        this.ficheiros = new HashMap<>();
        this.numEntradas = 0;
    }
    
    
    
}
