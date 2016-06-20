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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
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
    //自分が利用したピース一覧
    private ArrayList<String> usedPeices;
    //自分がまだ使えるピース一覧
    private ArrayList<String> havingPeices;
    //結果の出力先など
    private MessageRecevable userInterface;
    //このAIの名前
    
    private All All;

    public TajimaAI(Game game,All all) {
        super(game);
        this.TurnCount = 1;
        this.state = Game.STATE_WAIT_PLAYER_CONNECTION;
        this.usedPeices = new ArrayList<String>();
        this.havingPeices = new ArrayList<String>();
        for(String pcid:Piece.PieceIDList){
            this.havingPeices.add(pcid);
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
    private HashMap<String,ArrayList<Point>> getCanPutList(){
        HashMap<String,ArrayList<Point>> canPutList = new HashMap<String,ArrayList<Point>>();
        ArrayList<Point> list = BoardSub.ValidCornerTroutSet(this.myPlayerID,this.gameBoard.getBoardState());
        
        //最初だけ例外処理(ランダム用）
        if(this.TurnCount == 1){
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
        }else{
        //角リストをもとに、canPutListを生成する。
            for(String id:this.havingPeices){
                for (int d = 0; d < 8; d++) {
                    String fullID = id+"-"+d;
                    ArrayList<Point> putlist = new ArrayList<Point>();
                    for(Point pos:list){
                        Piece piece = new Piece(id,d);
                        int[][] pieceshape = piece.getPiecePattern();
                        int width = pieceshape[0].length;
                        int height = pieceshape.length;
                        int pieceMax = 0;
                        //ピースの長さが長い方を基準とする。
                        if(width > height){
                            pieceMax = width;
                        }else{
                            pieceMax = height;
                        }
                        //ピースの長さ分だけ、必要な探索を行う。
                        switch(pieceMax){
                            case 1:
                                putlist.add(new Point(pos.x,pos.y));
                                break;
                            case 2:
                                for (int i = pos.x-1; i <= pos.x+1; i++) {
                                    for (int j = pos.y-1; j <= pos.y+1; j++) {
                                        if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, piece, i,j) > 0){
                                            putlist.add(new Point(i,j));
                                        }
                                    }
                                    
                                }
                                break;
                            case 3:
                                for (int i = pos.x-2; i <= pos.x+2; i++) {
                                    for (int j = pos.y-2; j <= pos.y+2; j++) {
                                        if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, piece, i,j) > 0){
                                            putlist.add(new Point(i,j));
                                        }
                                    }
                                }
                                break;
                            case 4:
                                for (int i = pos.x-3; i <= pos.x+3; i++) {
                                    for (int j = pos.y-3; j <= pos.y+3; j++) {
                                        if(Math.abs(pos.x)+Math.abs(pos.y) > 4){
                                            if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, piece, i,j) > 0){
                                                putlist.add(new Point(i,j));
                                            }
                                        }
                                    }
                                }
                                break;
                            case 5:
                                if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, piece, pos.x-4,pos.y) > 0){
                                    putlist.add(new Point(pos.x-4,pos.y));
                                }
                                if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, piece, pos.x,pos.y-4) > 0){
                                    putlist.add(new Point(pos.x,pos.y-4));
                                }
                                if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, piece, pos.x+4,pos.y) > 0){
                                    putlist.add(new Point(pos.x+4,pos.y));
                                }
                                if(this.gameBoard.getBoard().canPutPiece(this.myPlayerID, piece, pos.x,pos.y+4) > 0){
                                    putlist.add(new Point(pos.x,pos.y+4));
                                }
                                break;
                            
                        }
                    }
                    if(putlist.size() > 0){
                        canPutList.put(fullID, putlist);
                    }
                }
            }
        }
        return canPutList;
    }
    
    /** ピースの配置を考えるメソッド
     * 引数1:HashMap<String,ArrayList<Point>> <置くピース,置ける点のリスト> canPutList
     * 返り値:String 置くピース
     */
    private String SelectPutPiece(HashMap<String,ArrayList<Point>> canPutList){
        String message;
        String[] pieceDataAndPoint;     //ピース番号分割用  pieceDataAndPoint[0] ピース番号-回転-X-Y　例：5A-3-1-2
        
        //canPutListに含まれるものがあれば、AIで思考 0ならばパス
        if(canPutList.keySet().size() > 0){
            //3と他の手
            if(TurnCount < 4){
                pieceDataAndPoint = this.theFirstThreeChoices();
                //this.nextPutAssess(pieceDataAndPoint);
                TurnCount++;
                message = finishMove(pieceDataAndPoint);
            }else{
                //3手以降の手を選択する。
                /*
                //ランダムセレクト用
                pieceDataAndPoint = this.randomSelect(canPutList);
                this.nextPutAssess(pieceDataAndPoint);
                message = finishMove(pieceDataAndPoint);
                */
                
                //ここからAI
                //評価リスト
                HashMap<String[],Integer> evaList = new HashMap<String[],Integer>();
                String[] pieceMaxpid;
                int pieceMaxValue;
                HashMap<String,ArrayList<Point>> canList = this.getCanPutList();
                
                //canPutListを評価し、評価リストevaList(Key:(String[]) pieceDataAndPoint  Value:(integer)評価値)を出力する
                for(String pid:canList.keySet()){
                    String canPieceName[] = pid.split("-");
                    //String配列に加えたりする用のArrayList
                    ArrayList<String> list = new ArrayList(Arrays.asList(canPieceName));
                    for(Point point:canList.get(pid)){
                        //リストにx,y座標を代入して評価、x,yを消去して再び評価を繰り返す
                        list.add(String.valueOf(point.x));
                        list.add(String.valueOf(point.y));
                        //評価をし、評価リストに代入
                        int AttackValue = PhaseShift();
                        System.out.println(this.gameBoard.getScore()[1]);
                        evaList.put(list.toArray(new String[0]),ValidAdjacentAssess(list.toArray(new String[0])));
                        list.remove(list.size()-1);
                        list.remove(list.size()-1);
                    }
                }
                
                //評価リストのうち、もっとも評価値が高いものを選定する
                pieceMaxValue = Collections.max(evaList.values());
                
                //最高評価値より低い物を削除
                for (Iterator<String[]> it = evaList.keySet().iterator(); it.hasNext();) {
                    String[] key = it.next();
                    if (evaList.get(key) < pieceMaxValue) {
                        it.remove();
                    }
                }

                //HashMapのKeyを1つランダムで選択する
                pieceMaxpid = this.getRandomHashMapKey(evaList);
                
                //評価値
                //System.out.println("Turn "+this.TurnCount+" 評価値:"+evaList.get(pieceMaxpid)+"  Piece:"+Arrays.asList(pieceMaxpid) ); 
                //System.out.println(this.havingPeices);
                
                message = finishMove(pieceMaxpid);
                TurnCount++;
            }
            
        } else {
            //パス
            message = passMove();
        }
        return message;
    }
    
    private int PhaseShift(){
        if(this.gameBoard.getScore()[1] > 45){return 5;
        }else{return 1;}
    }    
        
    private int ValidAdjacentAssess(String[] pieceDataAndPoint){
        Piece piece = new Piece(pieceDataAndPoint[0],Integer.parseInt(pieceDataAndPoint[1]));
        int x = Integer.parseInt(pieceDataAndPoint[2]);
        int y = Integer.parseInt(pieceDataAndPoint[3]);
        int[][] nowBoard = this.gameBoard.getBoardState();  //現在のボード A
        int[][] shadowBoard = new int[nowBoard.length][];   //未来のボード B
        
        //ディープコピー
        for (int i = 0; i < nowBoard.length; i++) {
            shadowBoard[i] = nowBoard[i].clone();
        }

        shadowBoard = BoardSub.putPiece(this.myPlayerID,piece,x,y,shadowBoard);
        
        ArrayList<Point> newPieceList = new ArrayList<Point>();
        for(int Py = 0;Py < BoardSub.BOARDSIZE;Py++){
            for(int Px = 0;Px < BoardSub.BOARDSIZE;Px++){
                if(nowBoard[Py][Px] != shadowBoard[Py][Px]) newPieceList.add(new Point(Px,Py));
            }
        }
        
        return BoardSub.ValidAdjacentSet(this.myPlayerID, nowBoard, newPieceList);
    }
    /** 評価関数 この関数で、手の評価を行う
     * 引数1:String[] pieceDataAndPoint
     * 返り値:int 評価値
     */
    private int nextPutAssess(String[] pieceDataAndPoint){
        Piece piece = new Piece(pieceDataAndPoint[0],Integer.parseInt(pieceDataAndPoint[1]));
        int x = Integer.parseInt(pieceDataAndPoint[2]);
        int y = Integer.parseInt(pieceDataAndPoint[3]);
        int[][] nowBoard = this.gameBoard.getBoardState();  //現在のボード A
        int[][] shadowBoard = new int[nowBoard.length][];   //未来のボード B
        int Katen;
        int Hatten;
        int evaluation;                                 //評価値
        ArrayList<Point> AandB = new ArrayList<Point>();
        ArrayList<Point> onlyA = new ArrayList<Point>();
        ArrayList<Point> onlyB = new ArrayList<Point>();
        //ディープコピー
        for (int i = 0; i < nowBoard.length; i++) {
            shadowBoard[i] = nowBoard[i].clone();
        }
        //現在と未来の角リストを作成する
        ArrayList<Point> nowCornerList;
        ArrayList<Point> shadowCornerList;
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
        
        
        //消えた手数を再評価
        for(Iterator<Point> it = onlyA.iterator(); it.hasNext();){
            Point Apiece = it.next();
            if(shadowBoard[Apiece.y][Apiece.x] != this.myPlayerID){
                it.remove();
            }
        }
        
        /*
        System.out.println("\n**************評価関数出力***************");
        System.out.println("Turn "+this.TurnCount);
        System.out.println(nowCornerList);
        System.out.println("置くピース:"+Arrays.toString(pieceDataAndPoint));
        System.out.println(shadowCornerList);
        System.out.println("onlyA:"+onlyA);
        System.out.println("onlyB:"+onlyB);
        //System.out.println("AandB:"+AandB);
        System.out.println("消えた手数（加点）："+onlyA.size());
        System.out.println("増える手数（発展）："+onlyB.size());
        
        System.out.println("***************************************\n");
        */
        
        //評価値
        //加点方法
        Katen = (onlyA.size()-1)*Integer.parseInt(pieceDataAndPoint[0].substring(0,1));
        //System.out.println(onlyA);
        //System.out.println(onlyA.size());
        Hatten = onlyB.size();
        
        if(Katen >= 5){
            evaluation = Katen;
            evaluation += Hatten; 
        }else{
           evaluation = Hatten;
        }
        
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
                super.gameBoard.setPlayerName(0,"RandomAI");
                super.gameBoard.setPlayerName(1,"opponent");
                this.state = Game.STATE_WAIT_PLAYER_PLAY;
            } else if(message.toUpperCase().equals("102 PLAYERID 1")){
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
}

