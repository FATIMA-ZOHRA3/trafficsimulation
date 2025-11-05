
public class Route {
    private int id;
    private String nom;
    private String points;
    private double longueur;
    private String typeRoute;
    private double vitesseMax;

    public Route(int id, String nom, String points, double longueur, String typeRoute, double vitesseMax) {
        this.id = id;
        this.nom = nom;
        this.points = points;
        this.longueur = longueur;
        this.typeRoute = typeRoute;
        this.vitesseMax = vitesseMax;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPoints() { return points; }
    public double getLongueur() { return longueur; }
    public String getTypeRoute() { return typeRoute; }
    public double getVitesseMax() { return vitesseMax; }
}
