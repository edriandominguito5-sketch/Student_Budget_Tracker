package org.yourcompany.yourproject;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            Website.start();
        } catch (IOException e) {
            System.out.println("Unable to start website: " + e.getMessage());
        }
    }
}
