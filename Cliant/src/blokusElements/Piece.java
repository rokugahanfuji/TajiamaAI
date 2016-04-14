/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blokusElements;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * ピースを表すためのクラス
 * インスタンスは１ピースを表すために使う
 * スタティックな要素としてすべての形などを定義してある。
 * @author ktajima
 */
public class Piece {
    
    private String PieceID;
    private int PieceCount;
    private int direction;
    private PiecePattern PPattern;

    public Piece(String id){
        this(id,0);
    }
    public Piece(String id,int d){
        //ID判定
        if(PieceIDList.contains(id)){
            this.PieceID = id;
            this.PieceCount = Integer.parseInt(id.substring(0, 1));
            this.direction = 0;
            this.PPattern = PieceList.get(id);
            //形状を確認する場合は下の行のコメントを解除すればよい
            //this.PPattern.printPieces();
            //
        } else {
            System.err.println(id+"は有効なピースのIDではありません");
            
        }
        if(d >= 0 && d < 8){
            this.direction = d;
        }
    }
    
    /** 現在のピースの形状を返す */
    public int[][] getPiecePattern() {
        return this.PPattern.getPiecePattern(this.direction);
    }
    
    /** ピースを回転させる */
    public void setDirction(int d) {
        if(d >= 0 && d < 8){
            this.direction = d;
        }
    }
    
    public String getPieceID(){
        return this.PieceID;
    }
    
    public String getPieceIDwithDirection(){
        return this.PieceID+"-"+this.direction;
    }
    
    public void printPiecePattern() {
        PPattern.printPieces(this.direction);
    }

    public int getPieceCount() {
        return this.PieceCount;
    }

    
    //静的(static)にすべてのピースを定義
    public static final HashMap<String,PiecePattern> PieceList = new HashMap<String,PiecePattern>();
    public static final ArrayList<String> PieceIDList = new ArrayList<String>();
    
    private static final int[][] P10_SHAPE = {   {1}};
    private static final int[][] P20_SHAPE = {   {1},
                                                {1}};
    private static final int[][] P30_SHAPE = {   {1},
                                                {1},
                                                {1}};
    private static final int[][] P31_SHAPE = {   {1,1},
                                                {1,0}};
    private static final int[][] P40_SHAPE = {   {0,1},
                                                {1,1},
                                                {1,0}};
    private static final int[][] P41_SHAPE = {   {1,1},
                                                {1,1}};
    private static final int[][] P42_SHAPE = {   {0,1,0},
                                                {1,1,1}};
    private static final int[][] P43_SHAPE = {   {1,1},
                                                {1,0},
                                                {1,0}};
    private static final int[][] P44_SHAPE = {   {1},
                                                {1},
                                                {1},
                                                {1}};
    private static final int[][] P50_SHAPE = {   {0,1,0,0},
                                                {1,1,1,1}};
    private static final int[][] P51_SHAPE = {   {0,1},
                                                {1,1},
                                                {1,1}};
    private static final int[][] P52_SHAPE = {   {0,1},
                                                {0,1},
                                                {1,1},
                                                {1,0}};
    private static final int[][] P53_SHAPE = {   {1,1},
                                                {1,0},
                                                {1,0},
                                                {1,0}};
    private static final int[][] P54_SHAPE = {   {1},
                                                {1},
                                                {1},
                                                {1},
                                                {1}};
    private static final int[][] P55_SHAPE = {   {1,1},
                                                {1,0},
                                                {1,1}};
    private static final int[][] P56_SHAPE = {   {0,1,1},
                                                {0,1,0},
                                                {1,1,0}};
    private static final int[][] P57_SHAPE = {   {0,1,1},
                                                {1,1,0},
                                                {1,0,0}};
    private static final int[][] P58_SHAPE = {   {0,0,1},
                                                {0,0,1},
                                                {1,1,1}};
    private static final int[][] P59_SHAPE = {   {0,0,1},
                                                {1,1,1},
                                                {0,0,1}};
    private static final int[][] P5A_SHAPE = {   {0,1,0},
                                                {0,1,1},
                                                {1,1,0}};
    private static final int[][] P5B_SHAPE = {   {0,1,0},
                                                {1,1,1},
                                                {0,1,0}};
    static {
        PieceList.put("10",new PiecePattern(P10_SHAPE));
        PieceIDList.add("10");
        
        PieceList.put("20",new PiecePattern(P20_SHAPE));
        PieceIDList.add("20");
        
        PieceList.put("30",new PiecePattern(P30_SHAPE));
        PieceList.put("31",new PiecePattern(P31_SHAPE));
        PieceIDList.add("30");
        PieceIDList.add("31");

        PieceList.put("40",new PiecePattern(P40_SHAPE));
        PieceList.put("41",new PiecePattern(P41_SHAPE));
        PieceList.put("42",new PiecePattern(P42_SHAPE));
        PieceList.put("43",new PiecePattern(P43_SHAPE));
        PieceList.put("44",new PiecePattern(P44_SHAPE));
        PieceIDList.add("40");
        PieceIDList.add("41");
        PieceIDList.add("42");
        PieceIDList.add("43");
        PieceIDList.add("44");

        PieceList.put("50",new PiecePattern(P50_SHAPE));
        PieceList.put("51",new PiecePattern(P51_SHAPE));
        PieceList.put("52",new PiecePattern(P52_SHAPE));
        PieceList.put("53",new PiecePattern(P53_SHAPE));
        PieceList.put("54",new PiecePattern(P54_SHAPE));
        PieceList.put("55",new PiecePattern(P55_SHAPE));
        PieceList.put("56",new PiecePattern(P56_SHAPE));
        PieceList.put("57",new PiecePattern(P57_SHAPE));
        PieceList.put("58",new PiecePattern(P58_SHAPE));
        PieceList.put("59",new PiecePattern(P59_SHAPE));
        PieceList.put("5A",new PiecePattern(P5A_SHAPE));
        PieceList.put("5B",new PiecePattern(P5B_SHAPE));
        PieceIDList.add("50");
        PieceIDList.add("51");
        PieceIDList.add("52");
        PieceIDList.add("53");
        PieceIDList.add("54");
        PieceIDList.add("55");
        PieceIDList.add("56");
        PieceIDList.add("57");
        PieceIDList.add("58");
        PieceIDList.add("59");
        PieceIDList.add("5A");
        PieceIDList.add("5B");
    }






    
    

}
