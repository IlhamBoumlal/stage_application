package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.Pharmacie;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PharmacieScraperService {

    public Set<String> getPharmaciesEnService() {
        Set<String> pharmaciesEnService = new HashSet<>();

        System.setProperty("webdriver.chrome.driver", "C:/Drivers/chromedriver-win64/chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        driver.get("https://www.annuaire-gratuit.ma/pharmacie-garde-fes.html");

        try {
            // Cibler uniquement les blocs <li> des pharmacies
            List<WebElement> pharmacies = driver.findElements(By.cssSelector("li.ag_listing_item"));

            for (WebElement pharmacie : pharmacies) {
                try {
                    WebElement nomElement = pharmacie.findElement(By.cssSelector("h3[itemprop='name']"));
                    String nom = nomElement.getText().trim();
                    if (!nom.isEmpty()) {
                        pharmaciesEnService.add(nom);
                    }
                } catch (Exception ignored) {
                    // Ignore si h3 non trouvé
                }
            }

        } finally {
            driver.quit();
        }

        return pharmaciesEnService;
    }

    public List<Pharmacie> getPharmaciesEnServiceSite() {
        List<Pharmacie> pharmaciesEnService = new ArrayList<>();

        // Assurez-vous que le chemin vers le driver est correct
        System.setProperty("webdriver.chrome.driver", "C:/Drivers/chromedriver-win64/chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        driver.get("https://www.annuaire-gratuit.ma/pharmacie-garde-fes.html");

        try {
            // Attendre un peu pour que la page se charge (peut être amélioré avec WebDriverWait)
            Thread.sleep(3000); // Simple attente, à remplacer par une attente explicite si possible

            List<WebElement> pharmacyElements = driver.findElements(By.cssSelector("li.ag_listing_item"));

            for (WebElement pharmacyElement : pharmacyElements) {
                try {
                    String nom = pharmacyElement.findElement(By.cssSelector("h3[itemprop='name']")).getText().trim();
                    String adresse = pharmacyElement.findElement(By.cssSelector("div[itemprop='address']")).getText().trim();
                    // Vous pouvez ajouter d'autres champs si disponibles

                    if (!nom.isEmpty()) {
                        pharmaciesEnService.add(new Pharmacie(nom, adresse));
                    }
                } catch (Exception e) {
                    // Loguer l'erreur ou ignorer si un élément n'est pas trouvé pour une pharmacie spécifique
                    System.err.println("Erreur lors de l'extraction d'une pharmacie : " + e.getMessage());
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Le scraping a été interrompu: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return pharmaciesEnService;
    }
/*
        public static void main(String[] args) {
        PharmacieScraperService scraper = new PharmacieScraperService();
        try {
            Set<String> pharmacies = scraper.getPharmaciesEnService();
            System.out.println("Pharmacies en garde : " + pharmacies);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

}
