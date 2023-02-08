できること

■ リモート画面は上部左の◀をクリックして表示される一覧から選ぶことができます。

■ リモート画面のズーム: 50～150%に対応しています。
  Autoではブラウザ画面の縦あるいは横の大きさで自動的に拡縮します。

■ ブラウザ内の背景色が薄緑の状態

- キー操作

　　ただしCTRL+DEL、ALT+TAB等の特殊な操作は、
　　ブラウザのOS側で有効となるため、リモート先には送信されません。
　　漢字の入力はできません。上部にある入力域に漢字文字列を
　　入力してからSendで送信してください。
　　キー配列は日本語配列対応となっています。

- マウス操作

　　リモート画面内のマウス位置は追従しません。
　　マウス位置を追従したい場合はCTRLキーを押しながら
　　マウスを移動してください。
　　（ドラッグ操作中はマウス位置が追従されます）

- マウス・ホイール操作

　　上下の移動に対応しています。
　　すばやい移動（2倍速）をしたい場合はCTRLまたはSHIFTを押しながら
　　ホイール操作をしてください。
　　CTRLとSHIFTを同時を押すと 4倍速 となります。

■ 今はできないこと（今後拡張するかは？）

- クリップボートの送受信
- ファイルの送受信
- リモート画面内のマウス形状への対応
- リモート画面（複数モニタ）への対応

■ 使うには

準備 1)　Webアプリ:monitorをTomcat等のJavaEEサーバーに配置します。
準備 2)　Tomcat等のJavaEEサーバーを起動します。
　　　　　　　（Tomcat等のポートは受信できるように設定してください）

準備 3)　リモート先(PC)上にJavaアプリ:clientを配置します。
準備 4)　Javaコマンド(Java 11以上)でclientを起動します。

	java -cp client.jar;javasysmon.jar Main
	
	デフォルトのWebサーバー（192.168.1.127:8080）以外を
	利用する場合は 第1引数に FQDN名/IPアドレス、
	利用ポートが8080以外は「:xxxx」を指定してください。
	
	java -cp client.jar;javasysmon.jar Main myserver1
	java -cpr client.jar;javasysmon.jar Main myserver1:8080

	java -cp client.jar;javasysmon.jar Main myserver2:80
	
	ブラウザの一覧に表示される名前はCOMPUTERNAMEとなります。
	特別な名前を表示したい場合は第2引数に指定してください。　　　   　　　

	java -cp client.jar;javasysmon.jar Main myserver1 my-computer

■ 参考　：　使った技術要素

【【【 client側 : 500行くらい 】】】

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.imageio.ImageIO;

【【【 monitor側 : 500行くらい 】】】

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

【【【 monitor側(Web) 】】】

- CSS
- HTML5
	- canvas
	- image
	- div/span他
- JavaScript : 300行くらい 
	- WebSocket
	- EventListener
	- timer他

■ 参考　：　動作概要

client -> monitor
	マウス位置をOS:Windowsから取得し、
	WebSocket（文字列）で随時送信
	Robot機能でWindowsスクリーン(Image)を取得し、
	Http Postで随時送信 
	
monitor -> browser 
	WebSocket（文字列）でマウス位置等を随時送信 
	Windowsスクリーン(Image)受信したらWebSocket（文字列）で再描画要を送信 

browser -> monitor 
	キー操作、マウス操作等をWebSocket（文字列）で随時送信 
	リモード画面(Image)をHttp Getで取得（canvas描画）

monitor -> client  
	キー操作、マウス操作等をWebSocket（文字列）で受信し

client -> OS:Windows   
	Robot機能でWindows OSへ通知
 