package com.licenta.ui;

import de.felixroske.jfxsupport.FXMLController;
import com.licenta.service.IService;
import javafx.fxml.FXML;
import org.springframework.beans.factory.annotation.Autowired;

@FXMLController
public class ProjectsController {



	@Autowired
	private IService service;

	@FXML
	public void initialize() {

		service.mahoot();

	}
}
