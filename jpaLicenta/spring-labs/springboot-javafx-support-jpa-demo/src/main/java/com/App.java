package com;


import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import com.licenta.ui.ProjectsView;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App extends AbstractJavaFxApplicationSupport {

  public static void main(String[] args) {
    launch(App.class, ProjectsView.class, args);
  }
}