package atj;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ManagerFile {
	private String filePath;
	private ByteBuffer buffer;
	
	ManagerFile(){
		buffer = null;
		filePath = null;
	}
	
	public void fileReceived(ByteBuffer buff) {
		buffer = buff;
		try {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Otrzymano plik");
			alert.setHeaderText("Czy chcesz pobrać plik?");

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				saveFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (filePath != null) {
			File file = new File(filePath);
			try {
				FileOutputStream ostream = new FileOutputStream(file, false);
				FileChannel channel = ostream.getChannel();

				channel.write(buffer);
				ostream.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	private void saveFile() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Gdzie chcesz zapisać plik?");
		
		Stage stage = new Stage();
		File path = chooser.showSaveDialog(stage);

		if (path != null) {
			filePath = path.toString();
		}
	}
}
