public class Imprimante extends Electronice
{
    private int ppm;
    private int dpi;
    private int p_car;
    private ModTiparire mod_tiparire;

    public Imprimante( String denumire, int nr_inv, double pret, String zona_magazin, Stare stare,
                      int ppm, int dpi, int p_car, ModTiparire mod_tiparire) {
        super("IMPRIMANTA", denumire, nr_inv, (float) pret, zona_magazin , stare);
        this.ppm = ppm;
        this.dpi = dpi;
        this.p_car = p_car;
        this.mod_tiparire = mod_tiparire;
        this.setStare(stare);
        this.mod_tiparire = mod_tiparire;
    }

    public int getPpm() {
        return ppm;
    }

    public int getDpi() {
        return dpi;
    }

    public int getP_car() {
        return p_car;
    }

    public ModTiparire getMod_tiparire() {
        return mod_tiparire;
    }

    @Override
    public void AfisareEchipamente() {
        super.AfisareEchipamente();
    }
}
