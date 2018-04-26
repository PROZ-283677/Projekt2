package atj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ManagerFile {
	private String filePath;
	private File file;
	
	ManagerFile(){
		file = null;
		filePath = null;
	}
	
	public void fileReceived(File received) {
		file = received;
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
			File newFile = new File(filePath);
			
			try {
				FileChannel input = new FileInputStream(file).getChannel();
				FileChannel output = new FileOutputStream(newFile).getChannel();
				output.transferFrom(input, 0, input.size());
				input.close();
				output.close();
				Files.delete(file.toPath());
				file = null;

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				Files.delete(file.toPath());
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
