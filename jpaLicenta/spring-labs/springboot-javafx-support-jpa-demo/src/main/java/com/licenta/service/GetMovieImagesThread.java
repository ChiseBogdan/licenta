package com.licenta.service;

import com.licenta.exceptions.UnableToReachMovieURL;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class GetMovieImagesThread extends Thread {

    private Image[] finalImages;
    private String movieLink;
    private int index;
    private int resizingWidth = 150;
    private int resizingHeigth = 200;

    public GetMovieImagesThread(Image[] finalImages, String movieLink, int index){

        this.finalImages =finalImages;
        this.movieLink = movieLink;
        this.index = index;

    }

    private String getTheImageLinkFromAMovieURL(String movieURL){

        Document doc = null;
        String linkHref = null;
        try {
            doc = Jsoup.connect(movieURL).get();

            if(doc != null){
                Element imageElement = doc.select("head").first().select("link[rel=image_src]").first();
                linkHref = imageElement.attr("href");
            }


        } catch (IOException e) {
            throw new UnableToReachMovieURL("Unable to reach movie url: " + movieURL, e);
        }

        return linkHref;

    }

    private Image resize(Image imageToResize){

        BufferedImage img = SwingFXUtils.fromFXImage(imageToResize, null);
        java.awt.Image tmp = img.getScaledInstance(resizingWidth, resizingHeigth, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(resizingWidth, resizingHeigth, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return SwingFXUtils.toFXImage(resized, null);

    }

    @Override
    public void run() {


        String imageHref = getTheImageLinkFromAMovieURL(movieLink);
        Image image = new Image(imageHref);
        Image resizedImage = resize(image);
        finalImages[index] = resizedImage;

    }
}
