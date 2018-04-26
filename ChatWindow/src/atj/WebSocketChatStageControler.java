package atj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class WebSocketChatStageControler {
	@FXML
	TextField userTextField;
	@FXML
	TextArea chatTextArea;
	@FXML
	TextField messageTextField;
	@FXML
	Button btnSet;
	@FXML
	Button btnSend;
	@FXML
	Button selectFile;
	
	private String user;
	private WebSocketClient webSocketClient;
	private String content; //wiadomosc ktora przesylamy (ciag znakow)
	private boolean isAttachment; // mowi czy chcemy wyslac plik
	private File file; // jesli zalaczymy plik to mamy go tu
	private static final int SIZE = 1024*1024; // rozmiar bufora 1MB
	
	@FXML
	private void initialize() {
		webSocketClient = new WebSocketClient();
		user = userTextField.getText();
		content = null;
		file = null;
		isAttachment = false;
		
		selectFile.setDisable(true);
		btnSend.setDisable(true);
	}

	@FXML
	private void btnSet_Click() {
		if (userTextField.getText().isEmpty() || user.equals(userTextField.getText())) return;
		user = userTextField.getText();

		selectFile.setDisable(false);
		btnSend.setDisable(false);
	}

	@FXML
	private void btnSend_Click() {
		if (messageTextField.getText().isEmpty()) return; 
		content = messageTextField.getText();
		webSocketClient.sendMessage(content);
		messageTextField.clear();
	}
	
	@FXML private void textFieldEnter(KeyEvent e) {
		if(e.getCode() == KeyCode.ENTER) {
			btnSend_Click();
		}
	}
	
	@FXML
	private void btnSelectFile() {
		Stage stage = new Stage();
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Dodaj załącznik");
		file = chooser.showOpenDialog(stage);
		if(file == null) 
			isAttachment = false;
		else {
			isAttachment = true;
			messageTextField.insertText(0, "Attached [FILE]: "+ file.getName());
		}
	}

	public void closeSession(CloseReason closeReason) {
		try {
			webSocketClient.session.close(closeReason);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@ClientEndpoint
	public class WebSocketClient {
		private Session session;

		public WebSocketClient() {
			connectToWebSocket();
		}

		@OnOpen
		public void onOpen(Session session) {
			System.out.println("Connection is opened");
			this.session = session;
		}

		@OnClose
		public void onClose(CloseReason closeReason) {
			System.out.println("Connection is closed: " + closeReason.getReasonPhrase());
		}

		@OnError
		public void onError(Throwable throwable) {
			System.out.println("Error");
			throwable.printStackTrace();
		}

		@OnMessage
		public void onMessage(String message, Session session) {
			System.out.println("Message was received");
			chatTextArea.setText(chatTextArea.getText() + message + "\n");
		}
		
		@OnMessage
		public void onMessage(ByteBuffer buffer, Session session) {
			File tmpFile = new File(session.getId()); // dzieki temu, jesli odbiore kilka paczek pod rzad od tego samego hosta to nie utworze nowego pliku

			if(buffer.capacity() > SIZE) {  // jesli przyszla ostatnia czesc pliku to wywolaj ze odebrano plik
				System.out.println("File was received");
				
				try {
					ManagerFile manager = new ManagerFile();
					Platform.runLater(() -> manager.fileReceived(tmpFile));

				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}
			else {
				try {
					FileOutputStream ostream = new FileOutputStream(tmpFile, true);
					FileChannel channel = ostream.getChannel();
					channel.write(buffer);
					channel.close();
					ostream.close();
				} catch (IOException i) {
					i.printStackTrace();
				}
			}
		}

		private void connectToWebSocket() {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:8080/WebSocketPROZ/websocketendpoint");
				webSocketContainer.connectToServer(this, uri);
			} catch (DeploymentException | IOException e) {
				e.printStackTrace();
			}
		}

		public void sendMessage(String message) {
			if(isAttachment == false) {
				try {
					System.out.println("Message was sent: " + message);
					session.getBasicRemote().sendText(user + ": " + message);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			else {
				long fileLength = file.length();
				
				try {
					ByteBuffer byteBuffer;
					InputStream istream = new FileInputStream(file);
					byte[] buffer = new byte[SIZE];
					
					if(fileLength > SIZE){
						byteBuffer = ByteBuffer.allocateDirect(SIZE);
						
						while (fileLength >= SIZE) {
							istream.read(buffer);
							byteBuffer.put(buffer);
							byteBuffer.flip();
							session.getBasicRemote().sendBinary(byteBuffer);
							byteBuffer.clear();
							
							fileLength -= SIZE;
						}
						
					}
					if(fileLength != 0) { // jesli plik jest mniejszy niz 1MB lub zostanie czesc <1MB po petli wyzej
						buffer = new byte[(int)fileLength];
						byteBuffer = ByteBuffer.allocateDirect((int)fileLength);
						
						istream.read(buffer);
						byteBuffer.put(buffer);
						byteBuffer.flip();
						session.getBasicRemote().sendBinary(byteBuffer);
						byteBuffer.clear();
					}
					istream.close();
					
					byteBuffer = ByteBuffer.allocateDirect(SIZE+1);
					session.getBasicRemote().sendBinary(byteBuffer);
					session.getBasicRemote().sendText("[FILE] sent by "+ user +": "+ file.getName());
					
				} catch (IOException ex) {
					ex.printStackTrace();
				}

				isAttachment = false;
				file = null;
			}
		}
	} // public class WebSocketClient
} // public class WebSocketChatStageControler