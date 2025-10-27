public class Electronice {
    private String Tip;
    private String Denumire;
    private int nr_inv;
    private float Pret;
    private String zona_magazin;
    private Stare stare;

    public Electronice(String Tip, String Denumire, int nr_inv, float Pret, String zona_magazin, Stare stare) {
        this.Tip = Tip;
        this.Denumire = Denumire;
        this.nr_inv = nr_inv;
        this.Pret = Pret;
        this.zona_magazin = zona_magazin;
        this.stare = stare; 
    }

    public String getTip() {
        return Tip;
    }
    public String getDenumire() {
        return Denumire;
    }
    public int getNr_inv() {
        return nr_inv;
    }
    public float getPret() {
        return Pret;
    }
    public String getZona_magazin() {
        return zona_magazin;
    }
    public Stare getStare() {
        return stare;
    }

    public void setTip(String Tip) {  this.Tip = Tip;
    }
    public void setDenumire(String Denumire) {
        this.Denumire = Denumire;
    }
    public void setNr_inv(int nr_inv) {
        this.nr_inv = nr_inv;}
    public void setPret(float Pret) {
        this.Pret = Pret;}
    public void setZona_magazin(String zona_magazin) {
        this.zona_magazin = zona_magazin;
    }
    public void setStare(Stare stare) {
        this.stare = stare;
    }

    public void AfisareEchipamente()
    {
        System.out.printf("Tip: %s | Denumire: %s | Numar Inventar: %d | Pret: %.2f | Zona Magazin: %s | Stare: %s\n",
                getTip(), getDenumire(), getNr_inv(), getPret(), getZona_magazin(), getStare());
    }

}
