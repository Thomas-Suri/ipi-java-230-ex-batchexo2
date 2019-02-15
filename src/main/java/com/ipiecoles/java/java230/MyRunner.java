package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.LocalDate;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
        //readFile(strings[0]);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) {
        Stream<String> stream = null;
        //Catcher
        try{
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        }
        catch(IOException e){
            logger.error("Problème dans l'ouverture du fichier "+ fileName);
            return null;
        }
       // stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        //Afficher chaque ligne du fichier dans la console
        List<String> lignes = stream.collect(Collectors.toList());
        for(int i = 0; 1 < lignes.size(); i++){
            System.out.println(lignes.get(i));
            try {
                processLine(lignes.get(i));
            }
             catch(BatchException e){
                logger.error("Ligne "+ (i+1)+ ":"+ e.getMessage() + " => " + lignes.get(i));
            }
        }

           /* logger.error("Ceci est une erreur");
            logger.warn("Ceci est un avertissement");
            logger.info("Ceci est une info");*/

        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        switch (ligne.substring(0,1)){
            case "T" :
                processTechnicien(ligne);
                break;
            case "M" :
                processManager(ligne);
                break;
            case "C" :
                processCommercial(ligne);
                break;
            default:
                throw new BatchException("Type d'employer inconnu : " + ligne.substring(0,1));
        }
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {

        String[] cL = ligneCommercial.split(",");
        verifChamps(cL.length,NB_CHAMPS_COMMERCIAL, "commercial");
        String mat = verifMatricule(cL[0], REGEX_MATRICULE);
        LocalDate embauche = verifDate(cL[3]);
        double salaire = salaire(cL[4]);
        double chiffreAffaire = cA(cL[5]);
        int performance = perf(cL[6]);

        Commercial com = new Commercial(cL[1], cL[2], mat, embauche, salaire, chiffreAffaire, performance);
        employeRepository.save(com);
    }
    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        String[] cL = ligneManager.split(",");
        verifChamps(cL.length,NB_CHAMPS_MANAGER, "manager");
        String mat = verifMatricule(cL[0], REGEX_MATRICULE_MANAGER);
        LocalDate embauche = verifDate(cL[3]);
        double salaire = salaire(cL[4]);

        Manager mana = new Manager(cL[1], cL[2], mat, embauche, salaire, null);
        employeRepository.save(mana);
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        String[]cL = ligneTechnicien.split(",");
        verifChamps(cL.length,NB_CHAMPS_TECHNICIEN, "technicien");
        String mat = verifMatricule(cL[0], REGEX_MATRICULE);
        LocalDate embauche = verifDate(cL[3]);
        double salaire = salaire(cL[4]);
        int grd = grade(cL[5]);
        String matManager = verifMatricule(cL[6], REGEX_MATRICULE_MANAGER);
        managions(matManager);

        try {
            Technicien tech = new Technicien(cL[1], cL[2], mat, embauche, salaire, grd);
            employeRepository.save(tech);
        }
        catch (TechnicienException e) {
            throw new BatchException(e.getMessage());
        }
    }

    private String verificationMatricule(String mat, String reg) throws BatchException {
        if (mat.matches(reg)) {
            return mat;
        }
        else {
            throw new BatchException("La chaîne " + mat + " ne respecte pas l'expression régulière " + reg + ".");
        }
    }

    private LocalDate verificationDate(String date) throws BatchException {
        try {
            LocalDate d = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(date);
            return d;
        }
        catch (Exception e) {
            throw new BatchException("La " + date + " ne respecte pas le format de date dd/MM/yyyy.");
        }
    }

    private void verificationChamps(int c, int tailleChamps, String typeEmploye) throws BatchException {
        if (c != tailleChamps) {
            throw new BatchException("La ligne " + typeEmploye + " ne contient pas " + tailleChamps + " éléments mais " + c + ".");
        }

    }

    public double verficationSalaire(String salaire) throws BatchException {
        try {
            double pay = Double.parseDouble(salaire);
            return pay;
        }
        catch (Exception e) {
            throw new BatchException("Le " + salaire + " n'est pas un nombre valide pour un salaire.");
        }
    }

    public double verificationChiffreAffaire(String chiffreAffaire) throws BatchException {
        try {
            double cA = Double.parseDouble(chiffreAffaire);
            return cA;
        }
        catch (Exception e) {
            throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + cA + ".");
        }
    }
    public int verificationPerformance(String performance) throws BatchException {
        try {
            int perf = Integer.parseInt(performance);
            return perf;
        }
        catch (Exception e) {
            throw new BatchException("La performance du commercial est incorrecte : " + perf + ".");
        }
    }
    public int verificationGrade(String grd) throws BatchException {
        try {
            int grade = Integer.parseInt(grd);
            return grade;
        }
        catch (Exception e) {
            throw new BatchException("Le grade du technicien est incorrect : " + grd + ".");
        }
    }
    private void verificationMan(String matricule) throws BatchException {
        Employe e = employeRepository.findByMatricule(matricule);
        if (e == null) {
            throw new BatchException("Le manager de matricule " + matricule + " n'a pas été trouvé dans le fichier ou en base de données.");
        }

    }


}
