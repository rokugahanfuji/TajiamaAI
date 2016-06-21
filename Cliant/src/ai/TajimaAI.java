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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import network.ServerConnecter;
import simpleclient.All;
/**
 *
 * @author Admin
 */
public class TajimaAI extends BlokusAI{
    private static final String AINAME = "Taji";

    //自分自身の処理状態
    private int state;
    //サーバとの接続を保持するクラス
    private ServerConnecter connecter;
    //自分に割り当てられたID（接続前は-1）
    private int myPlayerID = -1;
    private int enemyPlayerID = -1;
    //自分が利用したピース一覧
    private ArrayList<String> usedPeices;
    private ArrayList<String> enemyusedPeices;
    //自分がまだ使えるピース一覧
    private ArrayList<String> havingPeices;
    private ArrayList<String> enemyhavingPeices;
    //結果の出力先など
    private MessageRecevable userInterface;
    
    //打ちたい手のEntry型ArrayList
    private ArrayList<Entry<Integer,ArrayList<String[]>>> evaluationArrayListPlayer = new ArrayList<Entry<Integer,ArrayList<String[]>>>();
    private ArrayList<Entry<Integer,ArrayList<String[]>>> evaluationArrayListEnemy = new ArrayList<Entry<Integer,ArrayList<String[]>>>();
    
    //private HashMap<String[],Integer> evaluationHashMapEnemy = new HashMap<String[],Integer>();
    private HashMap<Integer,ArrayList<String[]>> evaluationHashMapPlayer = new HashMap<Integer,ArrayList<String[]>>();
    private HashMap<Integer,ArrayList<String[]>> evaluationHashMapEnemy = new HashMap<Integer,ArrayList<String[]>>();

    private All All;

    public TajimaAI(Game game,All all) {
        super(game);
        this.TurnCount = 1;
        this.state = Game.STATE_WAIT_PLAYER_CONNECTION;
        this.usedPeices = new ArrayList<String>();
        this.havingPeices = new ArrayList<String>();
        this.enemyusedPeices = new ArrayList<String>();
        this.enemyhavingPeices = new ArrayList<String>();
        for(String pcid:Piece.PieceIDList){
            this.havingPeices.add(pcid);
            this.enemyhavingPeices.add(pcid);
        }
        
        this.All = all;
    }

    public int[][] recevedData;
    public int receveline;
    private int TurnCount;
    private boolean Result;
    
    //メッセージ解析用の正規表現パターン
    private Pattern PLAYEDMSGPTN = Pattern.compile("401 PLAYED ([0-1]) (1?[0-9]) (1?[0-9]) ([0-5][0-9A-F])-([0-8])");
    private Pattern PASSEDMSGPTN = Pattern.compile("402 PASSED ([0-1])");

    /**** 主要な変数
    * String[] pieceDataAndPoint
    * ピースの情報と、その打つ場所を持った配列
    * [0] ピース番号     例:5A
    * [1] ピースの回転角  例:4
    * [2] 打つX座標      例:4
    * [3] 打つY座標      例:5
    * 例では、5A-4のピースを、(4,5)に打つ手を示す。
     
    * String pidPoint
    * pieceDataAndPointの配列を、"-"で繋いだ文字列
    * 例:5A-4-3-5
    
    * String[]pdata,String pid ---> [0][1]のみcanPutListで使用
    
    * HashMap<String,ArrayList<Point>> canPutList 
    * <pid,打てる座標のPoint型のリスト>
    * 例 Key  :5A-3
    *   Value:{(1,3),(4,5),(2,5),(2,7)}
    * 
    *****/

    /** 配置可能なピースのIDとその場所の一覧を取得する
     * 返り値:HashMap<String,ArrayList<Point>> <置くピース,置ける点のリスト> canPutList
     */
    private HashMap<String,ArrayList<Point>> getCanPutList(int PlayerID){
        HashMap<String,ArrayList<Point>> canPutPointHashMap = new HashMap<String,ArrayList<Point>>();
        ArrayList<Point> validCornerPointArrayList = BoardSub.ValidCornerTroutSet(PlayerID,this.gameBoard.getBoardState());
        
        //最初だけ例外処理(ランダム用）
        if(this.TurnCount == 1){
            for(String pieceId:this.havingPeices){
                for(int pieceAngle=0;pieceAngle<8;pieceAngle++){
                    String pieceIdFull = pieceId+"-"+pieceAngle;
                    ArrayList<Point> putPointArrayList = new ArrayList<Point>();
                    for(int x=0;x<Board.BOARDSIZE;x++){
                        for(int y=0;y<Board.BOARDSIZE;y++){
                            if(this.gameBoard.getBoard().canPutPiece(PlayerID, new Piece(pieceId,pieceAngle), x, y) > 0){
                                putPointArrayList.add(new Point(x,y));
                            }
                        }
                    }
                    if(putPointArrayList.size() > 0){
                        canPutPointHashMap.put(pieceIdFull, putPointArrayList);
                    }
                }
            }
        }else{
        //角リストをもとに、canPutListを生成する。
            for(String pieceId:this.havingPeices){
                for (int pieceAngle = 0; pieceAngle < 8; pieceAngle++) {
                    String pieceIdFull = pieceId+"-"+pieceAngle;
                    ArrayList<Point> putPointArrayList = new ArrayList<Point>();
                    for(Point pos:validCornerPointArrayList){
                        Piece piece = new Piece(pieceId,pieceAngle);
                        int[][] pieceShape = piece.getPiecePattern();
                        int pieceWidth = pieceShape[0].length;
                        int pieceHeight = pieceShape.length;
                        int pieceLength = 0;
                        //ピースの長さが長い方を基準とする。
                        if(pieceWidth > pieceHeight){
                            pieceLength = pieceWidth;
                        }else{
                            pieceLength = pieceHeight;
                        }
                        //ピースの長さ分だけ、必要な探索を行う。
                        switch(pieceLength){
                            case 1:
                                putPointArrayList.add(new Point(pos.x,pos.y));
                                break;
                            case 2:
                                for (int i = pos.x-1; i <= pos.x+1; i++) {
                                    for (int j = pos.y-1; j <= pos.y+1; j++) {
                                        if(this.gameBoard.getBoard().canPutPiece(PlayerID, piece, i,j) > 0){
                                            putPointArrayList.add(new Point(i,j));
                                        }
                                    } 
                                }
                                break;
                            case 3:
                                for (int i = pos.x-2; i <= pos.x+2; i++) {
                                    for (int j = pos.y-2; j <= pos.y+2; j++) {
                                        if(this.gameBoard.getBoard().canPutPiece(PlayerID, piece, i,j) > 0){
                                            putPointArrayList.add(new Point(i,j));
                                        }
                                    }
                                }
                                break;
                            case 4:
                                for (int i = pos.x-3; i <= pos.x+3; i++) {
                                    for (int j = pos.y-3; j <= pos.y+3; j++) {
                                        if(Math.abs(pos.x)+Math.abs(pos.y) > 4){
                                            if(this.gameBoard.getBoard().canPutPiece(PlayerID, piece, i,j) > 0){
                                                putPointArrayList.add(new Point(i,j));
                                            }
                                        }
                                    }
                                }
                                break;
                            case 5:
                                if(this.gameBoard.getBoard().canPutPiece(PlayerID, piece, pos.x-4,pos.y) > 0){
                                    putPointArrayList.add(new Point(pos.x-4,pos.y));
                                }
                                if(this.gameBoard.getBoard().canPutPiece(PlayerID, piece, pos.x,pos.y-4) > 0){
                                    putPointArrayList.add(new Point(pos.x,pos.y-4));
                                }
                                if(this.gameBoard.getBoard().canPutPiece(PlayerID, piece, pos.x+4,pos.y) > 0){
                                    putPointArrayList.add(new Point(pos.x+4,pos.y));
                                }
                                if(this.gameBoard.getBoard().canPutPiece(PlayerID, piece, pos.x,pos.y+4) > 0){
                                    putPointArrayList.add(new Point(pos.x,pos.y+4));
                                }
                                break;
                        }
                    }
                    if(putPointArrayList.size() > 0){
                        canPutPointHashMap.put(pieceIdFull, putPointArrayList);
                    }
                }
            }
        }
        return canPutPointHashMap;
    }
    
    /** ピースの配置を考えるメソッド
     * 引数1:HashMap<String,ArrayList<Point>> <置くピース,置ける点のリスト> canPutList
     * 返り値:String 置くピース
     */
    private String SelectPutPiece(HashMap<String,ArrayList<Point>> canPutList,int PlayerID){
        String message;
        String[] pieceDataAndPointArray;     //ピース番号分割用  pieceDataAndPoint[0] ピース番号-回転-X-Y　例：5A-3-1-2
        
        //canPutListに含まれるものがあれば、AIで思考 0ならばパス
        if(canPutList.keySet().size() > 0){
            //3と他の手
            if(TurnCount < 4){
                pieceDataAndPointArray = this.theFirstThreeChoices();
                //this.nextPutAssess(pieceDataAndPoint);
                TurnCount++;
                message = finishMove(pieceDataAndPointArray);
            }else{
                //3手以降の手を選択する。
                /*
                //ランダムセレクト用
                pieceDataAndPoint = this.randomSelect(canPutList);
                this.nextPutAssess(pieceDataAndPoint);
                message = finishMove(pieceDataAndPoint);
                */
                
                //ここからAI
                
                String[] pieceIdArrayMaxEvaluationPlayer;
                String[] pieceIdArrayMaxEvaluationEnemy;
                int evaluationValueMaxPlayer = 0;
                int evaluationValueMaxEnemy;
                Entry<String[],Integer> evaluationEntryPlayer;
                Entry<String[],Integer> evaluationEntryEnemy;
                
                HashMap<String,ArrayList<Point>> canPutPieceHashMapPlayer = this.getCanPutList(this.myPlayerID);
                HashMap<String,ArrayList<Point>> canPutPieceHashMapEnemy = this.getCanPutList(this.enemyPlayerID);
                
                //[敵]canPutPieceの結果をもとに評価リストevaluationHashMapを生成する
                this.evaluationHashMapEnemy = new HashMap<Integer,ArrayList<String[]>>();
                //打ちたい手のHashMap
                if(canPutPieceHashMapEnemy.isEmpty() != true){
                    for(String pieceId:canPutPieceHashMapEnemy.keySet()){
                        ArrayList<String> canPutPieceIdArrayList = new ArrayList(Arrays.asList(pieceId.split("-")));
                        for(Point canPutPiecePoint:canPutPieceHashMapEnemy.get(pieceId)){
                            canPutPieceIdArrayList.add(String.valueOf(canPutPiecePoint.x));
                            canPutPieceIdArrayList.add(String.valueOf(canPutPiecePoint.y));
                            evaluationPushToHashMap(canPutPieceIdArrayList.toArray(new String[0]), nextPutAssess(canPutPieceIdArrayList.toArray(new String[0]),enemyPlayerID),enemyPlayerID);
                            canPutPieceIdArrayList.remove(canPutPieceIdArrayList.size()-1);
                            canPutPieceIdArrayList.remove(canPutPieceIdArrayList.size()-1);
                        }
                    }  
                    /*
                    for(Entry<Integer,ArrayList<String[]>> entry:evaluationHashMapPlayer.entrySet()){
                        System.out.print(entry.getKey()+":");
                        System.out.println(Arrays.asList(entry.getValue())+":");
                    }
                    */
                    
                    //evaluationHashMapをEntryにして配列PutListEnemyに代入し、ソートまで行う
                    this.evaluationArrayListEnemy = new ArrayList<Entry<Integer,ArrayList<String[]>>>(this.evaluationHashMapEnemy.entrySet());
                    this.evaluationSort(this.evaluationArrayListEnemy);
                }
                
                //[味方]canPutPieceの結果をもとに評価リストevaluationHashMapを生成する
                this.evaluationHashMapPlayer = new HashMap<Integer,ArrayList<String[]>>();
                //canPutListを評価し、評価リストevaList(Key:(String[]) pieceDataAndPoint  Value:(integer)評価値)を出力する
                for(String pieceId:canPutPieceHashMapPlayer.keySet()){
                    ArrayList<String> canPutPieceIdArrayList = new ArrayList(Arrays.asList(pieceId.split("-")));
                    for(Point canPutPiecePoint:canPutPieceHashMapPlayer.get(pieceId)){
                        canPutPieceIdArrayList.add(String.valueOf(canPutPiecePoint.x));
                        canPutPieceIdArrayList.add(String.valueOf(canPutPiecePoint.y));
                        evaluationPushToHashMap(canPutPieceIdArrayList.toArray(new String[0]), nextPutAssess(canPutPieceIdArrayList.toArray(new String[0]),PlayerID),PlayerID);
                        canPutPieceIdArrayList.remove(canPutPieceIdArrayList.size()-1);
                        canPutPieceIdArrayList.remove(canPutPieceIdArrayList.size()-1);
                    }
                }  
                
                //evaluationHashMapをEntryにして配列PutListEnemyに代入し、ソートまで行う
                this.evaluationArrayListPlayer = new ArrayList<Entry<Integer,ArrayList<String[]>>>(this.evaluationHashMapPlayer.entrySet());
                this.evaluationSort(this.evaluationArrayListPlayer);

                pieceIdArrayMaxEvaluationPlayer = this.getMaxEvaluationList(evaluationArrayListPlayer);                    
                    
                //評価値
                System.out.println("Turn "+this.TurnCount+" 評価値:"+this.evaluationArrayListPlayer.get(0).getKey()+"  Piece:"+Arrays.asList(pieceIdArrayMaxEvaluationPlayer) ); 
                System.out.println(this.havingPeices);
                
                message = finishMove(pieceIdArrayMaxEvaluationPlayer);
                TurnCount++;
            }
            
        } else {
            //パス
            message = passMove();
        }
        return message;
    }
    
    /** 評価関数 この関数で、手の評価を行う
     * 引数1:String[] pieceDataAndPoint
     * 返り値:int 評価値
     */
    private int nextPutAssess(String[] pieceDataAndPoint,int PlayerID){
        Piece pieceIdAndAngle = new Piece(pieceDataAndPoint[0],Integer.parseInt(pieceDataAndPoint[1]));
        int piecePlaceX = Integer.parseInt(pieceDataAndPoint[2]);               
        int piecePlaceY = Integer.parseInt(pieceDataAndPoint[3]);
        int[][] nowABoard = this.gameBoard.getBoardState();                     //現在のボード A
        int[][] shadowBBoardPlayer = new int[nowABoard.length][];               //自分手未来のボード B
        int[][] shadowBBoardEnemy = new int[nowABoard.length][];                //相手手未来のボード B
        int gainAdd;                                                            //自分点数加点度
        int gainReduce;                                                         //相手点数削減度
        int gainGrowth;                                                         //自分手発展度
        int gainRecession;                                                      //相手手削減度
        int evaluation = 0;                                                     //評価値
        ArrayList<Point> AandBPointArrayListPlayer = new ArrayList<Point>();
        ArrayList<Point> onlyAPointArrayListPlayer = new ArrayList<Point>();
        ArrayList<Point> onlyBPointArrayListPlayer = new ArrayList<Point>();
        ArrayList<Point> AandBPointArrayListEnemy = new ArrayList<Point>();
        ArrayList<Point> onlyAPointArrayListEnemy = new ArrayList<Point>();
        ArrayList<Point> onlyBPointArrayListEnemy = new ArrayList<Point>();
        //ディープコピー
        for (int i = 0; i < nowABoard.length; i++) {
            shadowBBoardPlayer[i] = nowABoard[i].clone();
            shadowBBoardEnemy[i] = nowABoard[i].clone();
        }
        //現在と未来の角リストを作成する
        ArrayList<Point> nowCornerPointArrayListPlayer;
        ArrayList<Point> shadowCornerPointArrayListPlayer;
        ArrayList<Point> nowCornerPointArrayListEnemy;
        ArrayList<Point> shadowCornerPointArrayListEnemy;
        shadowBBoardPlayer = BoardSub.putPiece(PlayerID,pieceIdAndAngle,piecePlaceX,piecePlaceY,shadowBBoardPlayer);
        if(PlayerID == 0){
            shadowBBoardEnemy = BoardSub.putPiece(1, pieceIdAndAngle, piecePlaceX, piecePlaceY, shadowBBoardEnemy);
        }else{
            shadowBBoardEnemy = BoardSub.putPiece(0, pieceIdAndAngle, piecePlaceX, piecePlaceY, shadowBBoardEnemy);
        }

        //それぞれ評価する
        nowCornerPointArrayListPlayer = BoardSub.ValidCornerTroutSet(PlayerID,nowABoard);
        shadowCornerPointArrayListPlayer = BoardSub.ValidCornerTroutSet(PlayerID,shadowBBoardPlayer);
        if(PlayerID == 0){
            nowCornerPointArrayListEnemy = BoardSub.ValidCornerTroutSet(1, nowABoard);
            shadowCornerPointArrayListEnemy = BoardSub.ValidCornerTroutSet(1, shadowBBoardEnemy);
        }else{
            nowCornerPointArrayListEnemy = BoardSub.ValidCornerTroutSet(0, nowABoard);
            shadowCornerPointArrayListEnemy = BoardSub.ValidCornerTroutSet(0, shadowBBoardEnemy);
        }
        //System.out.println(rivalCornerList);
        
        for(Point shadowCornerPointEnemy:shadowCornerPointArrayListEnemy){
            onlyBPointArrayListEnemy.add(shadowCornerPointEnemy);
        }
        for(Point shadowCornerPointPlayer:shadowCornerPointArrayListPlayer){
            onlyBPointArrayListPlayer.add(shadowCornerPointPlayer);
        }
        
        //それぞれを評価
        for(Point shadowCornerPointEnemy:nowCornerPointArrayListEnemy){
            if(shadowCornerPointArrayListEnemy.contains(shadowCornerPointEnemy)){
                AandBPointArrayListEnemy.add(shadowCornerPointEnemy);
                onlyBPointArrayListEnemy.remove(shadowCornerPointEnemy);
            }else{
                onlyAPointArrayListEnemy.add(shadowCornerPointEnemy);
            }
        }
        for(Point shadowCornerPointPlayer:nowCornerPointArrayListPlayer){
            //AandB
            if(shadowCornerPointArrayListPlayer.contains(shadowCornerPointPlayer)){
                AandBPointArrayListPlayer.add(shadowCornerPointPlayer);
                onlyBPointArrayListPlayer.remove(shadowCornerPointPlayer);
            }else{
                onlyAPointArrayListPlayer.add(shadowCornerPointPlayer);
            }
        }
        
        //消えた手数を再評価
        for(Iterator<Point> it = onlyAPointArrayListPlayer.iterator(); it.hasNext();){
            Point piecePointAPlayer = it.next();
            if(shadowBBoardPlayer[piecePointAPlayer.y][piecePointAPlayer.x] != PlayerID){
                it.remove();
            }
        }
        for(Iterator<Point> it = onlyAPointArrayListEnemy.iterator(); it.hasNext();){
            Point piecePointAEnemy = it.next();
            if(PlayerID == 0){
               if(shadowBBoardEnemy[piecePointAEnemy.y][piecePointAEnemy.x] != 1){
                   it.remove();
               }
            }else{
                if(shadowBBoardEnemy[piecePointAEnemy.y][piecePointAEnemy.x] != 0){
                   it.remove();
               }
            }
        }
        
        /*if(onlyrA.size() >= 1){
            System.out.println("\n**************評価関数出力***************");
            System.out.println("Turn "+this.TurnCount);
            System.out.println(rivalCornerList);
            System.out.println("置くピース:"+Arrays.toString(pieceDataAndPoint));
            System.out.println(rshadowCornerList);
            System.out.println("onlyrA:"+onlyrA);
            System.out.println("onlyrB:"+onlyrB);
            System.out.println("AandB:"+AandB);
            System.out.println("消えた手数（相手）："+onlyrA.size());
            System.out.println("増える手数（発展）："+onlyB.size());

            System.out.println("***************************************\n");
        }
        */
        //評価値
        //加点方法
        gainAdd = (onlyAPointArrayListPlayer.size()-1)*Integer.parseInt(pieceDataAndPoint[0].substring(0,1));
        gainReduce = onlyAPointArrayListEnemy.size();
        //System.out.println(Genten);
        //System.out.println(onlyA);
        //System.out.println(onlyA.size());
        gainGrowth = onlyBPointArrayListPlayer.size();
        gainRecession = onlyBPointArrayListEnemy.size();
        
        //if(Katen >= 5){
            //evaluation = Katen;
            //evaluation += Hatten; 
        //}else{
           //evaluation = Hatten;
        //}
        
        //if(Genten == 0){
        //    evaluation = Katen + Hatten;
        //}else{
        //    evaluation = Genten;
        //}
        if(gainReduce > 0){
            evaluation = gainReduce * 100 + gainAdd + gainGrowth;
        }else{
            evaluation = gainAdd + gainGrowth;
        }
        //System.out.println(evaluation);
        return evaluation;
    }
    
    /** 手をランダムに打つ関数
     * 引数1:HashMap<String,ArrayList<Point>> <置くピース,置ける点のリスト> canPutList
     * 返り値:String[] pieceDataAndPoint
     */
    private String[] randomSelect(HashMap<String,ArrayList<Point>> canPutList){
        String pid;
        String[] pdata;
        String[] ids;
        Piece putPiece;
        Point putPlace;
        ArrayList<Point> points;
        
        //canPutList -> Key:57-2 Value:Array(ピースが置ける位置)
        //rd  -> random
        //ids -> canPutListキーを配列
        //pid -> ピースID 例；"57-2" （初期ではランダム）
        //pieceDataAndPoint[0] -> "57" ピース形 
        //pieceDataAndPoint[1] -> "2"  回転  
        //putPiece  pieceDataAndPointをもとに作成したPiece型
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
    
    /** HashMapのランダムなキーを取得する
     * ピースの評価値が同じ時、ランダムで選択する関数
     * 引数1:HashMap<String[],Integer> <置くピース,評価値> 
     * 返り値:int 評価値
     */
    private String[] getRandomHashMapKey(HashMap<String[],Integer> evaList){
        ArrayList<String[]> ranList = new ArrayList<String[]>();
        for(String[] pid:evaList.keySet()){
            ranList.add(pid);
        }
        
        Collections.shuffle(ranList);
        return ranList.get(0);
    }
    
    /** ターン終了処理
     * 引数1:String[] pieceDataAndPoint[]
     * 返り値:String メッセージ 
     */
    private String finishMove(String[] pieceDataAndPoint){
        String pid = pieceDataAndPoint[0] + "-" + pieceDataAndPoint[1];
        Point putPlace = new Point(Integer.parseInt(pieceDataAndPoint[2]), Integer.parseInt(pieceDataAndPoint[3]));
        Piece putPiece = new Piece(pieceDataAndPoint[0],Integer.parseInt(pieceDataAndPoint[1]));   //Piece型

        //自分のデータを更新し、サーバにもデータを送る
        this.gameBoard.play(this.myPlayerID, putPiece, putPlace.x, putPlace.y);
        this.usedPeices.add(pieceDataAndPoint[0]);
        this.havingPeices.remove(pieceDataAndPoint[0]);
        String message = "405 PLAY "+putPlace.x+" "+putPlace.y+" "+pid;
        
        return message;
    }
    
    
    /** ターン終了（パス）処理 **/
    private String passMove(){
        this.gameBoard.pass(this.myPlayerID);
        String message = "406 PASS";
        
        return message;
    }
    
    /** 決め打ち用の関数 **/
    private String[] theFirstThreeChoices(){
        String[] pieceDataAndPoint = new String[4];
            switch(TurnCount){
                case 1:
                    if(this.myPlayerID == 0){
                        pieceDataAndPoint = "5A-1-0-0".split("-");
                    } else {
                        pieceDataAndPoint = "5A-4-12-12".split("-");
                    }
                    break;
                case 2:
                    if(this.myPlayerID == 0){
                        pieceDataAndPoint = "5B-0-2-2".split("-");
                    } else {
                        pieceDataAndPoint = "5B-0-10-10".split("-");
                    }
                    break;
                case 3:
                    if(this.myPlayerID == 0){
                        pieceDataAndPoint = "57-0-4-4".split("-");
                    } else {
                        pieceDataAndPoint = "57-2-8-8".split("-");
                    }
                    break;      
            }
            return pieceDataAndPoint;
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
            } else if(message.toUpperCase().equals("102 PLAYERID 0")){
                //先手の場合
                this.myPlayerID = 0;
                this.enemyPlayerID = 1;
                super.gameBoard.setPlayerName(0,"RandomAI");
                super.gameBoard.setPlayerName(1,"opponent");
                this.state = Game.STATE_WAIT_PLAYER_PLAY;
            } else if(message.toUpperCase().equals("102 PLAYERID 1")){
                //後手の場合
                this.myPlayerID = 1;
                this.enemyPlayerID = 0;
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
                this.enemyusedPeices.add(PieceID);
                this.enemyhavingPeices.remove(PieceID);
                System.out.println("ok");
                int PieceDirection = Integer.parseInt(mc.group(5));
                this.gameBoard.play(pid, new Piece(PieceID,PieceDirection) , x, y);
            } else if(mc2.matches()){//402 PASSED [01]
                int pid = Integer.parseInt(mc2.group(1));
                this.gameBoard.pass(pid);
            } else if(message.toUpperCase().equals("404 DOPLAY")){
                
                // おくことができるピースとその場所を確認（総当りで探します）
                HashMap<String,ArrayList<Point>> canPutList = getCanPutList(this.myPlayerID);
                
                // サーバに送るメッセージを決定
                //このメソッドを修正すれば、最低限の自分なりのプログラムが作れます。
                String sendmessage = SelectPutPiece(canPutList,this.myPlayerID);
                this.connecter.sendMessage(sendmessage);
                this.userInterface.addMessage(sendmessage);
            } else if(message.toUpperCase().equals("501 WINNER 0")){ 
                if(this.myPlayerID == 0){
                    this.Result = true;
                }else{
                    this.Result = false;
                }
            } else if(message.toUpperCase().equals("501 WINNER 1")){ 
                if(this.myPlayerID == 1){
                    this.Result = true;
                }else{
                    this.Result = false;
                }
            } else if(message.toUpperCase().equals("502 GAME END")){
                //自動化の処理
                if(this.All.getJidouFlag() == true && this.All.getMakeFlag() == true){
                    //負けてて負けチェック入ってたらリフレッシュ
                    if(this.Result == true){
                        this.All.Reflesh();
                    }
                } else if(this.All.getJidouFlag() == true){
                    //自動化処理
                        this.All.Reflesh();
                }
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
    
    @Override
    public void initForReflesh(Game game){
        this.gameBoard = game;
        this.TurnCount = 1;
        this.state = Game.STATE_WAIT_PLAYER_CONNECTION;
        this.usedPeices = new ArrayList<String>();
        this.havingPeices = new ArrayList<String>();
        for(String pcid:Piece.PieceIDList){
            this.havingPeices.add(pcid);
        }
        
    }

    //ArrayList<Entry>をValue値でソートを行う
    private void evaluationSort(ArrayList<Entry<Integer,ArrayList<String[]>>> List) {
        Collections.sort(List, new Comparator<Entry<Integer,ArrayList<String[]>>>() {
            //比較関数
            @Override
            public int compare(Entry<Integer,ArrayList<String[]>> o1, Entry<Integer,ArrayList<String[]>> o2) {
                //return o1.getValue().compareTo(o2.getValue());    //昇順
                return o2.getKey().compareTo(o1.getKey());    //降順
            }
        });
    }
    
    //降順ソート済みのArrayList<Entry>内の最大値IDをランダムで取得し、Entryを返却
    private String[] getMaxEvaluationList(ArrayList<Entry<Integer,ArrayList<String[]>>> SortList){
        //最大値リスト
        
        return SortList.get(0).getValue().get((int)(Math.random() * SortList.get(0).getValue().size()));
    }

    private void evaluationPushToHashMap(String[] PieceID, int evaluation,int PlayerID) {
        if(PlayerID == this.myPlayerID){
            //すでに評価値が存在する場合は取り出して代入する
            if(this.evaluationHashMapPlayer.containsKey(evaluation)){
                this.evaluationHashMapPlayer.get(evaluation).add(PieceID);
            }else{
                ArrayList<String[]> list = new ArrayList<String[]>();
                list.add(PieceID);
                this.evaluationHashMapPlayer.put(evaluation,list);
            }
        }else{
            //すでに評価値が存在する場合は取り出して代入する
            if(this.evaluationHashMapEnemy.containsKey(evaluation)){
                this.evaluationHashMapEnemy.get(evaluation).add(PieceID);
            }else{
                ArrayList<String[]> list = new ArrayList<String[]>();
                list.add(PieceID);
                this.evaluationHashMapEnemy.put(evaluation,list);
            }
        }
    }
    
}




