public class SistemCalculatoare extends Electronice
{
    private String tip_mon;
    private double vit_proc;
    private int c_hdd;
    private SistemOperare sistem_operare;

    public SistemCalculatoare(String denumire, int nr_inv, double pret, String zona_magazin, Stare stare,
                              String tip_mon, double vit_proc, int c_hdd, SistemOperare sistem_operare) {
        super("SISTEM", denumire, nr_inv, (float) pret, zona_magazin,stare);
        this.tip_mon = tip_mon;
        this.vit_proc = vit_proc;
        this.c_hdd = c_hdd;
        this.sistem_operare = sistem_operare;
        this.setStare(stare);
    }

    public String getTip_mon() {
        return tip_mon;
    }

    public double getVit_proc() {
        return vit_proc;
    }

    public int getC_hdd() {
        return c_hdd;
    }

    public SistemOperare getSistem_operare() {
        return sistem_operare;
    }

    @Override
    public void AfisareEchipamente() {
        super.AfisareEchipamente();
    }
}
