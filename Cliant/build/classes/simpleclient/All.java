/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simpleclient;

import ai.BlokusAI;
import ai.TajimaAI;
import blokusElements.Game;
import gui.ClientGUI;



/**
 *
 * @author Tsutomu
 */
public class All {
    private Game myGame;
    private BlokusAI   myAI;
    private ClientGUI myGUI;
    
    public All(){
    }
    
    public void SetAll(Game Game,BlokusAI AI,ClientGUI GUI){
        this.myGame = Game;
        this.myAI = AI;
        this.myGUI = GUI;    
    }
    
    public void Reflesh(){
        this.myGame.initForReflesh();
        this.myAI.initForReflesh(myGame);
        
        this.myGUI.RefleshText();
    }
    
    public boolean getJidouFlag(){
        return this.myGUI.getJidouFlag();
    }
    
    public boolean getMakeFlag(){
        return this.myGUI.getMakeFlag();
    }
}
