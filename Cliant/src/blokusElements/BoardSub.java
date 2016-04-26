/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blokusElements;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author fujisawa
 */
public class BoardSub extends Board {
    public BoardSub(){
        
    }
    
    public static int[][] putPiece(int player,Piece piece,int x,int y,int[][] exBoard){
        int[][] pieceshape = piece.getPiecePattern();
        int width = pieceshape[0].length;
        int height = pieceshape.length;

        try{
            //ボード状態を書き換える
            for(int i=0;i<width;i++){
                for(int j=0;j<height;j++){
                    if(pieceshape[j][i] == 1){
                        exBoard[y+j][x+i] = player;
                    }
                }
            }
        } catch(java.lang.ArrayIndexOutOfBoundsException e){
            //e.printStackTrace();
            return null;
        }

        return exBoard;
    }
    
    //playerIDに対応した、有効な角リストをArrayList<Point>で返却
    public static ArrayList ValidCornerTroutSet(int playerID,int[][] boardState){
        int i,j; 
        ArrayList<Point> ValidCornerTroutList = new ArrayList<Point>();
        Point ValidCornerTroutPoint;
        for(i=0;i<BOARDSIZE;i++){
            for(j=0;j<BOARDSIZE;j++){
                ValidCornerTroutPoint = new Point();
                if((i-1 > -1) && (j > 1) && (i < BOARDSIZE-1) && (j < BOARDSIZE-1)){
                    if(boardState[i][j+1] == -1 && boardState[i][j-1] == -1 && boardState[i-1][j] == -1 && boardState[i+1][j] == -1){
                        if(boardState[i-1][j-1] == playerID || boardState[i+1][j-1] == playerID || boardState[i-1][j+1] == playerID || boardState[i+1][j+1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }else if((i < 1) && (j > 1) && (i < BOARDSIZE-1) && (j < BOARDSIZE-1)){
                    if(boardState[i][j+1] == -1 && boardState[i][j-1] == -1 && boardState[i+1][j] == -1){
                        if(boardState[i+1][j-1] == playerID || boardState[i+1][j+1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }else if((i > 1) && (j < 1) && (i < BOARDSIZE-1) && (j < BOARDSIZE-1)){
                    if(boardState[i][j+1] == -1 && boardState[i-1][j] == -1 && boardState[i+1][j] == -1){
                        if(boardState[i-1][j+1] == playerID || boardState[i+1][j+1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }else if((i > 1) && (j > 1) && (i == BOARDSIZE-1) && (j < BOARDSIZE-1)){
                    if(boardState[i][j+1] == -1 && boardState[i][j-1] == -1 && boardState[i-1][j] == -1){
                        if(boardState[i-1][j-1] == playerID || boardState[i-1][j+1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }else if((i > 1) && (j > 1) && (i < BOARDSIZE-1) && (j == BOARDSIZE-1)){
                    if(boardState[i][j-1] == -1 && boardState[i-1][j] == -1 && boardState[i+1][j] == -1){
                        if(boardState[i-1][j-1] == playerID || boardState[i+1][j-1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }else if((i < 1) && (j < 1) && (i < BOARDSIZE-1) && (j < BOARDSIZE-1)){
                    if(boardState[i][j+1] == -1 && boardState[i+1][j] == -1){
                        if(boardState[i+1][j+1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }else if((i > 1) && (j < 1) && (i > BOARDSIZE-1) && (j < BOARDSIZE-1)){
                    if(boardState[i][j+1] == -1 && boardState[i-1][j] == -1){
                        if(boardState[i-1][j+1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }else if((i > 1) && (j > 1) && (i > BOARDSIZE-1) && (j > BOARDSIZE-1)){
                    if(boardState[i][j-1] == -1 && boardState[i-1][j] == -1){
                        if(boardState[i-1][j-1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }else if((i < 1) && (j > 1) && (i < BOARDSIZE-1) && (j > BOARDSIZE-1)){
                    if(boardState[i][j-1] == -1 && boardState[i+1][j] == -1){
                        if(boardState[i+1][j-1] == playerID){
                            ValidCornerTroutPoint.setLocation(j, i);
                            ValidCornerTroutList.add(ValidCornerTroutPoint);
                        }
                    }
                }
            }
        }
        return ValidCornerTroutList;
    }
    
}
