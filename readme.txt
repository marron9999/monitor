できること

■ リモート画面は上部左の◀をクリックして表示される一覧から選ぶことができます。

■ リモート画面のズーム: 50～150%に対応しています。
  Autoではブラウザ画面の縦あるいは横の大きさで自動的に拡縮します。

■ ブラウザ内の背景色が薄緑の状態

■ リモートとブラウザの間でファイルの送受信ができます

　　- リモート接続中にブラウザにファイルをドロップすると、リモート（Downloadsフォルダ下）に送信します。

　　- リモート内のアプルにファイルをドロップすると、ブラウザにファイル（Downloadsフォルダ下）に送信します。
　　　　ブラウザのリモート一覧の下にドロップだれたファイル一覧が表示され、
　　　　クリックするとテキストファイル等はブラウザで見ることができます。

■ リモート先のマウスカーソル形状が表示できます

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
- リモート画面（複数モニタ）への対応

■ 使うには

準備 1)　Webアプリ:monitorをTomcat等のJavaEEサーバーに配置します。
準備 2)　Tomcat等のJavaEEサーバーを起動します。
　　　　　　　（Tomcat等のポートは受信できるように設定してください）

準備 3)　リモート先(PC)上にJavaアプリ:clientを配置します。
準備 4)　Javaコマンド(Java 11以上)でclientを起動します。

	java -cp client.jar;javasysmon.jar Main
	
	デフォルトのWebサーバーが　localhost:8080　となっていますので
	利用するWebサーバーを 第1引数に FQDN名/IPアドレスを指定し、
	利用ポートが8080以外は「:xxxx」を付加して指定してください。
	
	java -cp <jars> Main myserver1
	java -cp <jars> Main myserver1:8080

	java -cp <jars> Main myserver2:80
	
	ブラウザの一覧に表示される名前はCOMPUTERNAMEとなります。
	特別な名前を表示したい場合は第2引数に指定してください。

	java -cp <jars> Main myserver1 my-computer

	注) <jars>は、以下を ; で連結した文字列です
		- client.jar
		- javasysmon.jar
		- jna-5.13.0.jar
		- jna-platform-5.13.0.jar

■ 参考　：　使った技術要素

【【【 client側 】】】

	client_imports.txt 参照

	javasysmon
	https://github.com/jezhumble/javasysmon

	Java Native Access (JNA)
	https://github.com/java-native-access/jna

【【【 monitor側 : 500行くらい 】】】

	monitor_imports.txt 参照

【【【 monitor側(Web) 】】】

- CSS
- HTML5
	- canvas
	- image
	- div/span他
- JavaScript
	- WebSocket
	- EventListener
	- DropTarget
	- XMLHttpRequest
	- timer他

■ 参考　：　動作概要

client -> monitor
	マウス位置をOS:Windowsから取得し、
	WebSocket（文字列）で随時送信
	マウス形状はOS:Windowsからイメージで取得し、
	Http Postで送信
	Robot機能でWindowsスクリーン(Image)を取得し、
	Http Postで随時送信
	
monitor -> browser 
	WebSocket（文字列）でマウス位置等を随時送信
	Windowsスクリーン(Image)受信したらWebSocket（文字列）で再描画要を送信
	マウス形状はimg srcにURLを設定(Http getで取得)

browser -> monitor 
	キー操作、マウス操作等をWebSocket（文字列）で随時送信
	リモード画面(Image)をHttp Getで取得（canvas描画）

monitor -> client  
	キー操作、マウス操作等をWebSocket（文字列）で受信

client -> OS:Windows
	Robot機能でWindows OSへ通知
 