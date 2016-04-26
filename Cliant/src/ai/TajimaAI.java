/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai;

import blokusElements.Game;
import blokusElements.Board;
import blokusElements.BoardSub;
import blokusElements.Game;
import blokusElements.Piece;
import gui.MessageRecevable;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import network.ServerConnecter;

/**
 *
 * @author Admin
 */
public class TajimaAI extends BlokusAI{
    private static final String AINAME = "TajimaAI";

    //自分自身の処理状態
    private int state;
    //サーバとの接続を保持するクラス
    private ServerConnecter connecter;
    //自分に割り当てられたID（接続前は-1）
    private int myPlayerID = -1;
    //自分が利用したピース一覧
    private ArrayList<String> usedPeices;
    //自分がまだ使えるピース一覧
    private ArrayList<String> havingPeices;
    //結果の出力先など
    private MessageRecevable userInterface;
    //このAIの名前

    public TajimaAI(Game game) {
        super(game);
        this.TurnCount = 0;
        this.state = Game.STATE_WAIT_PLAYER_CONNECTION;
        this.usedPeices = new ArrayList<String>();
        this.havingPeices = new ArrayList<String>();
        for(String pcid:Piece.PieceIDList){
            this.havingPeices.add(pcid);
        }
    }

    public int[][] recevedData;
    public int receveline;
    private int TurnCount;
    
    //メッセージ解析用の正規表現パターン
    private Pattern PLAYEDMSGPTN = Pattern.compile("401 PLAYED ([0-1]) (1?[0-9]) (1?[0-9]) ([0-5][0-9A-F])-([0-8])");
    private Pattern PASSEDMSGPTN = Pattern.compile("402 PASSED ([0-1])");

    /** 配置可能なピースのIDとその場所の一覧を取得する */
    private HashMap<String,ArrayList<Point>> getCanPutList(){
        HashMap<String,ArrayList<Point>> canPutList = new HashMap<String,ArrayList<Point>>();
        for(String id:this.havingPeices){
            for(int d=0;d<8;d++){
                String fullID = id+"-"+d;
                ArrayList<Point> putlist = new ArrayList<Point>();
                for(int x=0;x<Board.BOARDSIZE;x++){
                    for(int y=0;y<Board.BOARDSIZE;y++){
                        if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, new Piece(id,d), x, y) > 0){
                            putlist.add(new Point(x,y));
                        }
                    }
                }
                if(putlist.size() > 0){
                    canPutList.put(fullID, putlist);
                }
            }
        }
        return canPutList;
    }
    
    /** ピースの配置を考えるメソッド */
    private String SelectPutPiece(HashMap<String,ArrayList<Point>> canPutList){
        String message;
        String[] pdata = new String[4];     //ピース番号分割用  pdata[0] ピース番号-回転-X-Y　例：5A-3-1-2
        
        if(canPutList.keySet().size() > 0){
            //3と他の手
            if(TurnCount < 3){
                pdata = this.theFirstThreeChoices();
                this.nextPutAssess(pdata);
                TurnCount++;
            }else{
                //ランダムセレクト用
                pdata = this.randomSelect(canPutList);
                
                //ここからAI
                
                this.nextPutAssess(pdata);
                
                TurnCount++;
            }
            
            //手の評価を表示

            
            //ターン終了の処理
            message = finishMove(pdata);
        } else {
            //パス
            message = passMove();
        }
        return message;
    }
    
    
    //本来はcanputLISTが、自身の評価、nowCornerListになる。
    
    
    //次に置く手の評価を行う関数
    private int nextPutAssess(String[] pdata){
        Piece piece = new Piece(pdata[0],Integer.parseInt(pdata[1]));
        int x = Integer.parseInt(pdata[2]);
        int y = Integer.parseInt(pdata[3]);
        int[][] nowBoard = this.gameBoard.getBoardState();  //現在のボード A
        int[][] shadowBoard = new int[nowBoard.length][];   //未来のボード B
        int evaluation = 0;                                 //評価値
        ArrayList<Point> AandB = new ArrayList<Point>();
        ArrayList<Point> onlyA = new ArrayList<Point>();
        ArrayList<Point> onlyB = new ArrayList<Point>();
        //ディープコピー
        for (int i = 0; i < nowBoard.length; i++) {
            shadowBoard[i] = nowBoard[i].clone();
        }
        //現在と未来の角リストを作成する
        ArrayList<Point> nowCornerList = new ArrayList<Point>();
        ArrayList<Point> shadowCornerList = new ArrayList<Point>();
        shadowBoard = BoardSub.putPiece(this.myPlayerID,piece,x,y,shadowBoard);

        //それぞれ評価する
        nowCornerList = BoardSub.ValidCornerTroutSet(this.myPlayerID,nowBoard);
        shadowCornerList = BoardSub.ValidCornerTroutSet(this.myPlayerID,shadowBoard);
        
        for(Point value:shadowCornerList){
            onlyB.add(value);
        }
        
        //それぞれを評価
        for(Point value:nowCornerList){
            //AandB
            if(shadowCornerList.contains(value)){
                AandB.add(value);
                onlyB.remove(value);
            }else{
                onlyA.add(value);
            }
        }
        
        System.out.println(shadowCornerList);
        System.out.println(nowCornerList);
        System.out.println(AandB);
        System.out.println(onlyA);
        System.out.println(onlyB);
        
        
        System.out.println("Turn "+this.TurnCount);
        System.out.println(nowCornerList);
        System.out.println("置くピース:"+Arrays.toString(pdata));
        System.out.println(shadowCornerList);
        System.out.println("\n");
        /*
        Hashmap
        for (String Key : canPutList.keySet()){
            for (Point Value : canPutList.get(Key)){
                int[][] shadowBoard = this.gameBoard.getBoardState();
                
                
                
                System.out.println(Value);
                
            }
            
        }
        */

        //System.out.println("手によって減る数；");
        //System.out.println("手によって増える数；");
        return evaluation;
    }
    
    private ArrayList<String> boardCorner(int playerID,int[][] board){
        ArrayList<String> cornerList = new ArrayList<String>();
        BoardSub.ValidCornerTroutSet(playerID, board);
        
        return cornerList;
    }
    
    //ランダム処理
    private String[] randomSelect(HashMap<String,ArrayList<Point>> canPutList){
        String pid;
        String[] pdata;
        pdata = new String[4];
        String[] ids;
        Piece putPiece;
        Point putPlace = new Point();
        ArrayList<Point> points;
        
        //canPutList -> Key:57-2 Value:Array(ピースが置ける位置)
        //rd  -> random
        //ids -> canPutListキーを配列
        //pid -> ピースID 例；"57-2" （初期ではランダム）
        //pdata[0] -> "57" ピース形 
        //pdata[1] -> "2"  回転  
        //putPiece  pdataをもとに作成したPiece型
        //points -> pidをもとにcanPutList
        Random rd = new Random(System.currentTimeMillis());
        ids = canPutList.keySet().toArray(new String[0]);
        pid = ids[rd.nextInt(ids.length)];
        pdata = pid.split("-");
        putPiece = new Piece(pdata[0],Integer.parseInt(pdata[1]));
        points = canPutList.get(pid);
        putPlace = points.get(rd.nextInt(points.size()));
        ArrayList<String> list = new ArrayList(Arrays.asList(pdata));
        list.add(String.valueOf(putPlace.x));
        list.add(String.valueOf(putPlace.y));
        
        return (String[]) list.toArray(new String[0]);
    }
    
    //終了処理
    private String finishMove(String[] pdata){
        String pid = pdata[0] + "-" + pdata[1];
        Point putPlace = new Point(Integer.parseInt(pdata[2]), Integer.parseInt(pdata[3]));
        Piece putPiece = new Piece(pdata[0],Integer.parseInt(pdata[1]));   //Piece型

        //自分のデータを更新し、サーバにもデータを送る
        this.gameBoard.play(this.myPlayerID, putPiece, putPlace.x, putPlace.y);
        this.usedPeices.add(pdata[0]);
        this.havingPeices.remove(pdata[0]);
        String message = "405 PLAY "+putPlace.x+" "+putPlace.y+" "+pid;
        
        return message;
    }
    
    //パス処理
    private String passMove(){
        this.gameBoard.pass(this.myPlayerID);
        String message = "406 PASS";
        
        return message;
    }
    
    //3手返却用
    private String[] theFirstThreeChoices(){
        String[] pdata = new String[4];
            switch(TurnCount){
                case 0:
                    if(this.myPlayerID == 0){
                        pdata = "5A-1-0-0".split("-");
                    } else {
                        pdata = "5A-4-12-12".split("-");
                    }
                    break;
                case 1:
                    if(this.myPlayerID == 0){
                        pdata = "5B-0-2-2".split("-");
                    } else {
                        pdata = "5B-0-10-10".split("-");
                    }
                    break;
                case 2:
                    if(this.myPlayerID == 0){
                        pdata = "57-0-4-4".split("-");
                    } else {
                        pdata = "57-2-8-8".split("-");
                    }
                    break;      
            }
            return pdata;
    }

    /** このプログラムがサーバからメッセージを受信すると呼び出される */
    @Override
    public void getNewMessage(String message) {
        if(this.state == Game.STATE_WAIT_PLAYER_CONNECTION){
            if(message.toUpperCase().equals("100 HELLO")){
                //名前登録時の処理
                String sendmessage = "101 NAME "+AINAME;
                this.connecter.sendMessage(sendmessage);
                this.userInterface.addMessage(sendmessage);
            } else if(message.toUpperCase().equals("102 PLYERID 0")){
                //先手の場合
                this.myPlayerID = 0;
                super.gameBoard.setPlayerName(0,"RandomAI");
                super.gameBoard.setPlayerName(1,"opponent");
                this.state = Game.STATE_WAIT_PLAYER_PLAY;
            } else if(message.toUpperCase().equals("102 PLYERID 1")){
                //後手の場合
                this.myPlayerID = 1;
                super.gameBoard.setPlayerName(1,"RandomAI");
                super.gameBoard.setPlayerName(0,"opponent");
                this.state = Game.STATE_WAIT_PLAYER_PLAY;
            }
        } else if(this.state == Game.STATE_WAIT_PLAYER_PLAY){
            Matcher mc = PLAYEDMSGPTN.matcher(message);
            Matcher mc2 = PASSEDMSGPTN.matcher(message);
            if(mc.matches()){//401 PLAYED
                int pid = Integer.parseInt(mc.group(1));
                int x = Integer.parseInt(mc.group(2));
                int y = Integer.parseInt(mc.group(3));
                String PieceID = mc.group(4);
                int PieceDirection = Integer.parseInt(mc.group(5));
                this.gameBoard.play(pid, new Piece(PieceID,PieceDirection) , x, y);
            } else if(mc2.matches()){//402 PASSED [01]
                int pid = Integer.parseInt(mc2.group(1));
                this.gameBoard.pass(pid);
            } else if(message.toUpperCase().equals("404 DOPLAY")){
                
                // おくことができるピースとその場所を確認（総当りで探します）
                HashMap<String,ArrayList<Point>> canPutList = getCanPutList();
                
                // サーバに送るメッセージを決定
                //このメソッドを修正すれば、最低限の自分なりのプログラムが作れます。
                String sendmessage = SelectPutPiece(canPutList);
                this.connecter.sendMessage(sendmessage);
                this.userInterface.addMessage(sendmessage);
            }            
        }
    }
    
    @Override
    public void setConnecter(ServerConnecter c) {
        this.connecter = c;
    }

    @Override
    public void setOutputInterface(MessageRecevable mr) {
        this.userInterface = mr;
    }

    @Override
    public void reciveMessage(String text) {
        //メッセージを受信したときに呼び出される
        if(!super.isThinking){
            //思考モードでなければ処理を終了
            return;
        }
        this.getNewMessage(text);
    }

    @Override
    public void addMessage(String text) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

