/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

/**
 * GUI等に搭載するインタフェース　ログの追加と受信したメッセージの追加メソッドを持つ
 * @author koji
 */
public interface MessageRecevable {
    public void reciveMessage(String text);
    public void addMessage(String text);
}
